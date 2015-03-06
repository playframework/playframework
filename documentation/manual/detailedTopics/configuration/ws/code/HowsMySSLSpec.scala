package detailedtopics.configuration.ws {

// #context

import java.io.File

import play.api.{Mode, Environment}
import play.api.libs.json.JsSuccess
import play.api.libs.ws._
import play.api.libs.ws.ning._
import play.api.test._

import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import scala.concurrent.duration._

class HowsMySSLSpec extends PlaySpecification {

  def createClient(rawConfig: play.api.Configuration): WSClient = {
    val classLoader = Thread.currentThread().getContextClassLoader
    val parser = new WSConfigParser(rawConfig, new Environment(new File("."), classLoader, Mode.Test))
    val clientConfig = new NingWSClientConfig(parser.parse())
    // Debug flags only take effect in JSSE when DebugConfiguration().configure is called.
    //import play.api.libs.ws.ssl.debug.DebugConfiguration
    //clientConfig.ssl.map {
    //   _.debug.map(new DebugConfiguration().configure)
    //}
    val builder = new NingAsyncHttpClientConfigBuilder(clientConfig)
    val client = new NingWSClient(builder.build())
    client
  }

  def configToMap(configString: String): Map[String, _] = {
    import scala.collection.JavaConverters._
    ConfigFactory.parseString(configString).root().unwrapped().asScala.toMap
  }

  "WS" should {

    "verify common behavior" in {
      // GeoTrust SSL CA - G2 intermediate certificate not found in cert chain!
      // See https://github.com/jmhodges/howsmyssl/issues/38 for details.
      val geoTrustPem =
        """-----BEGIN CERTIFICATE-----
          |MIIEWTCCA0GgAwIBAgIDAjpjMA0GCSqGSIb3DQEBBQUAMEIxCzAJBgNVBAYT
          |AlVTMRYwFAYDVQQKEw1HZW9UcnVzdCBJbmMuMRswGQYDVQQDExJHZW9UcnVz
          |dCBHbG9iYWwgQ0EwHhcNMTIwODI3MjA0MDQwWhcNMjIwNTIwMjA0MDQwWjBE
          |MQswCQYDVQQGEwJVUzEWMBQGA1UEChMNR2VvVHJ1c3QgSW5jLjEdMBsGA1UE
          |AxMUR2VvVHJ1c3QgU1NMIENBIC0gRzIwggEiMA0GCSqGSIb3DQEBAQUAA4IB
          |DwAwggEKAoIBAQC5J/lP2Pa3FT+Pzc7WjRxr/X/aVCFOA9jK0HJSFbjJgltY
          |eYT/JHJv8ml/vJbZmnrDPqnPUCITDoYZ2+hJ74vm1kfy/XNFCK6PrF62+J58
          |9xD/kkNm7xzU7qFGiBGJSXl6Jc5LavDXHHYaKTzJ5P0ehdzgMWUFRxasCgdL
          |LnBeawanazpsrwUSxLIRJdY+lynwg2xXHNil78zs/dYS8T/bQLSuDxjTxa9A
          |kl0HXk7+Yhc3iemLdCai7bgK52wVWzWQct3YTSHUQCNcj+6AMRaraFX0DjtU
          |6QRN8MxOgV7pb1JpTr6mFm1C9VH/4AtWPJhPc48Obxoj8cnI2d+87FLXAgMB
          |AAGjggFUMIIBUDAfBgNVHSMEGDAWgBTAephojYn7qwVkDBF9qn1luMrMTjAd
          |BgNVHQ4EFgQUEUrQcznVW2kIXLo9v2SaqIscVbwwEgYDVR0TAQH/BAgwBgEB
          |/wIBADAOBgNVHQ8BAf8EBAMCAQYwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDov
          |L2NybC5nZW90cnVzdC5jb20vY3Jscy9ndGdsb2JhbC5jcmwwNAYIKwYBBQUH
          |AQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5nZW90cnVzdC5jb20w
          |TAYDVR0gBEUwQzBBBgpghkgBhvhFAQc2MDMwMQYIKwYBBQUHAgEWJWh0dHA6
          |Ly93d3cuZ2VvdHJ1c3QuY29tL3Jlc291cmNlcy9jcHMwKgYDVR0RBCMwIaQf
          |MB0xGzAZBgNVBAMTElZlcmlTaWduTVBLSS0yLTI1NDANBgkqhkiG9w0BAQUF
          |AAOCAQEAPOU9WhuiNyrjRs82lhg8e/GExVeGd0CdNfAS8HgY+yKk3phLeIHm
          |TYbjkQ9C47ncoNb/qfixeZeZ0cNsQqWSlOBdDDMYJckrlVPg5akMfUf+f1Ex
          |RF73Kh41opQy98nuwLbGmqzemSFqI6A4ZO6jxIhzMjtQzr+t03UepvTp+UJr
          |YLLdRf1dVwjOLVDmEjIWE4rylKKbR6iGf9mY5ffldnRk2JG8hBYo2CVEMH6C
          |2Kyx5MDkFWzbtiQnAioBEoW6MYhYR3TjuNJkpsMyWS4pS0XxW4lJLoKaxhgV
          |RNAuZAEVaDj59vlmAwxVG52/AECu8EgnTOCAXi25KhV6vGb4NQ==
          |-----END CERTIFICATE-----
        """.stripMargin

      val configString = """
         |//ws.ssl.debug=["certpath", "ssl", "trustmanager"]
         |ws.ssl.protocol="TLSv1"
         |ws.ssl.enabledProtocols=["TLSv1"]
         |
         |ws.ssl.trustManager = {
         |  stores = [
         |    { type: "PEM", data = ${geotrust.pem} }
         |  ]
         |}
       """.stripMargin
      val rawConfig = ConfigFactory.parseString(configString)
      val configWithPem = rawConfig.withValue("geotrust.pem", ConfigValueFactory.fromAnyRef(geoTrustPem))
      val configWithSystemProperties = ConfigFactory.load(configWithPem)
      val playConfiguration = play.api.Configuration(configWithSystemProperties)

      val client = createClient(playConfiguration)
      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(5.seconds)
      response.status must be_==(200)

      val jsonOutput = response.json
      val result = (jsonOutput \ "tls_version").validate[String]
      result must beLike {
        case JsSuccess(value, path) =>
          value must_== "TLS 1.0"
      }
    }
  }
}
// #context

}
