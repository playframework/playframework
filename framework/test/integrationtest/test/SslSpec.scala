import java.io.File
import java.net.{URL, URLConnection, Socket}
import java.security.cert.X509Certificate
import java.security.KeyStore
import javax.net.ssl.{HttpsURLConnection, SSLSocket, SSLContext, X509TrustManager}
import javax.security.auth.x500.X500Principal
import org.specs2.execute.{Result, AsResult}
import org.specs2.matcher.{Expectable, Matcher}
import org.specs2.mutable.{Around, Specification}
import org.specs2.specification.Scope
import play.api.test.{Helpers, FakeApplication, TestServer}
import play.core.server.netty.FakeKeyStore

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

  def createConn = {
    val conn = new URL("https://localhost:" + SslPort + "/json").openConnection().asInstanceOf[HttpsURLConnection]
    conn.setSSLSocketFactory(sslFactory)
    conn
  }

  def sslFactory = {
    val ctx = SSLContext.getInstance("TLS")
    ctx.init(null, Array(MockTrustManager()), null)
    ctx.getSocketFactory
  }

  case class MockTrustManager() extends X509TrustManager {
    val nullArray = Array[X509Certificate]()
    def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) {}
    def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) {}
    def getAcceptedIssuers = nullArray
  }
}
