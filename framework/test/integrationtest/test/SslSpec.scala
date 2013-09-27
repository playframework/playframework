import java.io.{InputStreamReader, FileInputStream, File}
import java.net.{HttpURLConnection, URL}
import java.security.cert.X509Certificate
import java.security.KeyStore
import javax.net.ssl._
import javax.security.auth.x500.X500Principal
import org.specs2.execute.{Result, AsResult}
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable.{Around, Specification}
import org.specs2.specification.Scope
import play.api.test.FakeApplication
import play.api.test.TestServer
import play.api.test.{Helpers, FakeApplication, TestServer}
import play.core.server.netty.FakeKeyStore
import scala.Some

class SslSpec extends Specification {

  val SslPort = 19443

  sequential

  "SSL support" should {
    "generate a self signed certificate when no keystore configuration is provided" in new Ssl {
      val conn = createConn
      conn.getResponseCode must_== 200
      conn.getPeerPrincipal must_== new X500Principal(FakeKeyStore.DnName)
    }

    "use a configured keystore" in new Ssl(keyStore = Some("conf/testkeystore.jks"), password = Some("password")) {
      val conn = createConn
      conn.getResponseCode must_== 200
      conn.getPeerPrincipal must_== new X500Principal("CN=localhost, OU=Unit Test, O=Unit Testers, L=Testland, ST=Test, C=TT")
    }
    "report a tampered keystore" in new Ssl(keyStore = Some("conf/badKeystore.jks"), password = Some("password")) {
      val conn = createConn
      conn.getResponseCode must throwA[java.io.IOException].like {
        case e => e.getMessage must startWith("Remote host closed connection during handshake")        
        /*
          What I'd really like to test for is that the log contains "Keystore was tampered with, or password was incorrect"
          but I don't know how to do that...
        */
      }
    }
    
  }

  abstract class Ssl(keyStore: Option[String] = None,
                     password: Option[String] = None,
                     trustStore: Option[String] = None) extends Around with Scope {

    implicit lazy val app = FakeApplication()

    override def around[T: AsResult](t: => T): Result = {
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
        Helpers.running(TestServer(Helpers.testServerPort, app, Some(SslPort)))(AsResult.effectively(t))
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

  def createConn = {
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
