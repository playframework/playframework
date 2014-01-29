package play.api.libs.ws.ssl

import play.api.test._
import play.api.libs.ws.WS
import play.api.test.TestServer
import play.api.test.FakeApplication
import play.api.mvc.{Results, Action}
import com.typesafe.config.ConfigFactory
import scala.util.control.NonFatal
import play.core.server.netty.FakeKeyStore

object WsSslSpec extends PlaySpecification with WsTestClient with Results {

  sequential

  "WS SSL" should {

    "run against a local test server configured with SSL" in {
      try {
        new java.io.File(FakeKeyStore.GeneratedKeyStore).delete()
        new java.io.File(FakeKeyStore.GeneratedTrustStore).delete()

        // FakeKeyStore should provide us with generated.keystore and generated.truststore, we cannot use OCSP here
        val config = """
                       |ws.ssl {
                       | loose.disableCheckRevocation = true
                       | keyManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.keystore, password: "" } ]
                       | }
                       | trustManager = {
                       |   stores = [ { type: "JKS", path: ./conf/generated.truststore, password: "" } ]
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
            // JSSE stacktraces are embarrassingly deep
            CompositeCertificateException.unwrap(e) { ex =>
              ex.printStackTrace()
            }
            failure
        }
      }
    }
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }

}
