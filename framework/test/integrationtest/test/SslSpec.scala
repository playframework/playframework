import java.io.{InputStreamReader, FileInputStream, File}
import java.net.{HttpURLConnection, URL}
import java.security.cert.X509Certificate
import java.security.KeyStore
import javax.net.ssl._
import javax.security.auth.x500.X500Principal
import org.apache.commons.io.IOUtils
import org.specs2.execute.Result
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable.{Around, Specification}
import org.specs2.specification.Scope
import play.api.test.{Helpers, FakeApplication, TestServer}
import play.core.server.netty.FakeKeyStore
import scala.Some

class SslSpec extends Specification {

  val SslPort = 19443

  sequential

  "SSL support" should {
    "generate a self signed certificate when no keystore configuration is provided" in new Ssl {
      val conn = jsonRequest
      conn.getResponseCode must_== 200
      conn.getPeerPrincipal must_== new X500Principal(FakeKeyStore.DnName)
    }

    "use a configured keystore" in new Ssl(keyStore = Some("conf/testkeystore.jks"), password = Some("password")) {
      val conn = jsonRequest
      conn.getResponseCode must_== 200
      conn.getPeerPrincipal must_== new X500Principal("CN=localhost, OU=Unit Test, O=Unit Testers, L=Testland, ST=Test, C=TT")
    }

    "support client certificates" in new Ssl(None, None, trustStore = Some("noCA")) {
      val conn = clientCertRequest()
      conn.getResponseCode must_== 200
      contentAsString(conn) must_== "Bob Client"
    }

    "not trust untrusted client certificates" in new Ssl(None, None, trustStore=Some("default")) {
      val conn = clientCertRequest()
      conn.getResponseCode must throwA[SSLHandshakeException]
    }

    "not accept no client certificate" in new Ssl(None, None, trustStore = Some("noCA")) {
      val conn = clientCertRequest(withClientCert = false)
      conn.getResponseCode must throwA[SSLHandshakeException]
    }

    "accept a trusted client certificate" in new Ssl(keyStore = Some("conf/testkeystore.jks"), password = Some("password"),
      trustStore = Some("keystore")) {
      val conn = clientCertRequest()
      conn.getResponseCode must_== 200
      contentAsString(conn) must_== "Bob Client"
    }
  }

  abstract class Ssl(keyStore: Option[String] = None,
                     password: Option[String] = None,
                     trustStore: Option[String] = None) extends Around with Scope {

    implicit lazy val app = FakeApplication()

    def around[T](t: => T)(implicit evidence: (T) => Result) = {
      val props = System.getProperties

      def setOrUnset(name: String, value: Option[String]) = value match {
        case Some(p) => props.setProperty(name, p)
        case None => props.remove(name)
      }

      // Make sure the generated keystore doesn't exist
      val generated = new File(FakeKeyStore.GeneratedKeyStore)
      if (generated.exists()) {
        generated.delete()
      }

      try {
        setOrUnset("https.keyStore", keyStore)
        setOrUnset("https.keyStorePassword", password)
        setOrUnset("https.trustStore", trustStore)
        Helpers.running(TestServer(Helpers.testServerPort, app, Some(SslPort)))(t)
      } finally {
        props.remove("https.keyStore")
        props.remove("https.keyStorePassword")
        props.remove("https.trustStore")
      }
    }
  }

  def contentAsString(conn: HttpURLConnection) = {
    resource.managed(new InputStreamReader(conn.getInputStream)).acquireAndGet { in =>
      val buf = new Array[Char](1024)
      var i = 0
      val answer = new StringBuffer()
      while( i!= -1) {
         answer.append(buf,0,i)
         i = in.read(buf)
      }
      answer.toString
    }
  }

  def jsonRequest = {
    val conn = new URL("https://localhost:" + SslPort + "/json").openConnection().asInstanceOf[HttpsURLConnection]
    conn.setSSLSocketFactory(sslFactory())
    conn
  }

  def clientCertRequest(withClientCert: Boolean = true) = {
    val conn = new URL("https://localhost:" + SslPort + "/clientCert").openConnection().asInstanceOf[HttpsURLConnection]
    conn.setSSLSocketFactory(sslFactory(withClientCert))
    conn
  }

  def sslFactory(withClientCert: Boolean = true) = {
    val kms = if (withClientCert) {
      val ks = KeyStore.getInstance("PKCS12")
      val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm)
      for (in <- resource.managed(new FileInputStream("conf/bobclient.p12"))) {
        ks.load(in, "password".toCharArray)
      }
      kmf.init(ks, "password".toCharArray)
      kmf.getKeyManagers
    } else {
      null
    }
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(kms, Array(MockTrustManager()), null)
    ctx.getSocketFactory
  }

  case class MockTrustManager() extends X509TrustManager {
    val nullArray = Array[X509Certificate]()
    def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}
    def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}
    def getAcceptedIssuers = nullArray
  }
}
