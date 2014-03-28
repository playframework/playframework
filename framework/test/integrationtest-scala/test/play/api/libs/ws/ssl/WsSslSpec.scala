package play.api.libs.ws.ssl

import play.api.libs.ws.WS
import play.api.test._
import play.api.mvc.{Results, Action}
import com.typesafe.config.ConfigFactory
import scala.util.control.NonFatal
import java.io.File
import play.core.server.netty.FakeKeyStore

object WsSslSpec extends PlaySpecification with WsTestClient with Results {

  sequential

    def changeLogLevel(loggerName: String) {
      import org.slf4j.LoggerFactory
      import ch.qos.logback.classic.Level
      import ch.qos.logback.classic.Logger

      val logger = LoggerFactory.getLogger(loggerName).asInstanceOf[Logger]
      logger.setLevel(Level.DEBUG)
    }

    "WS SSL" should {

      "must connect to server with no valid certificate if we have ws.acceptAnyCertificate defined" in {
        // Force a keystore to be generated even in TestServer.
        FakeKeyStore.keyManagerFactory(new File("."))

        // use the default trust store (that does NOT contain the trust server certificate)
        val config = """
                       |ws.acceptAnyCertificate = true
                       |ws.ssl {
                       | loose.disableCheckRevocation = true
                       | keyManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.keystore, password: "" } ]
                       | }
                       |}
                       | """.stripMargin

        // simplest possible app
        implicit val application = FakeApplication(additionalConfiguration = configToMap(config), withRoutes = {
          case ("GET", "/ssl") => Action {
            Ok("ok!")
          }
        })

        try {
          val server = TestServer(port = Helpers.testServerPort, application, sslPort = Some(11443))
          Helpers.running(server) {
            val client = WS.client
            await(client.url("https://localhost:11443/ssl").get()).status must_== 200
          }
          success
        } catch {
          case NonFatal(e) =>
            CompositeCertificateException.unwrap(e) {
              ex => ex.printStackTrace()
            }
            failure
        }
      }


      "must fail to connect to a local test server with no trusted certificate" in {
        // Force a keystore to be generated even in TestServer.
        FakeKeyStore.keyManagerFactory(new File("."))

        // use the default trust store (that does NOT contain the trust server certificate)
        val config = """
                       |ws.ssl {
                       | loose.disableCheckRevocation = true
                       | keyManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.keystore, password: "" } ]
                       | }
                       |}
                       | """.stripMargin

        // simplest possible app
        implicit val application = FakeApplication(additionalConfiguration = configToMap(config), withRoutes = {
          case ("GET", "/ssl") => Action {
            Ok("ok!")
          }
        })

        try {
          val server = TestServer(port = Helpers.testServerPort, application, sslPort = Some(11443))
          Helpers.running(server) {
            val client = WS.client
            await(client.url("https://localhost:11443/ssl").get()).status must_== 200
          }
          failure
        } catch {
          case NonFatal(e) =>
            success
        }
      }

      "run against a local test server configured with SSL" in {
        // Force a keystore to be generated even in TestServer.
        FakeKeyStore.keyManagerFactory(new File("."))

        // FakeKeyStore should provide us with generated.keystore and generated.truststore, we cannot use OCSP here
        val config = """
                       |ws.ssl {
                       | loose.disableCheckRevocation = true
                       | keyManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.keystore, password: "" } ]
                       | }
                       | trustManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.keystore, password: "" } ]
                       | }
                       |}
                       | """.stripMargin

        // simplest possible app
        implicit val application = FakeApplication(additionalConfiguration = configToMap(config), withRoutes = {
          case ("GET", "/ssl") => Action {
            Ok("ok!")
          }
        })

        try {
          val server = TestServer(port = Helpers.testServerPort, application, sslPort = Some(11443))
          Helpers.running(server) {
            val client = WS.client
            await(client.url("https://localhost:11443/ssl").get()).status must_== 200
          }
          success
        } catch {
          case NonFatal(e) =>
            CompositeCertificateException.unwrap(e) {
              ex => ex.printStackTrace()
            }
            failure
        }
      }
    }

    def configToMap(configString: String): Map[String, _] = {
      import scala.collection.JavaConverters._
      ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
    }

}
