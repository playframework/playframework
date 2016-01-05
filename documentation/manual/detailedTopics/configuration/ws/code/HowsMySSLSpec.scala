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
      // Digital Signature Trust Co certificate is not in the JDK trust chain.
      val dstRootCa = """|-----BEGIN CERTIFICATE-----
        |MIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/
        |MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT
        |DkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow
        |PzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD
        |Ew5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB
        |AN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O
        |rz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq
        |OLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b
        |xiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw
        |7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD
        |aeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV
        |HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG
        |SIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69
        |ikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr
        |AvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz
        |R8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5
        |JDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo
        |Ob8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ
        |-----END CERTIFICATE-----
        |""".stripMargin

      val configString = """
         |//play.ws.ssl.debug=["certpath", "ssl", "trustmanager"]
         |play.ws.ssl.protocol="TLSv1"
         |play.ws.ssl.enabledProtocols=["TLSv1"]
         |
         |play.ws.ssl.trustManager = {
         |  stores = [
         |    { type: "PEM", data = ${dstRootCa.pem} }
         |  ]
         |}
       """.stripMargin
      val rawConfig = ConfigFactory.parseString(configString)
      val configWithPem = rawConfig.withValue("dstRootCa.pem", ConfigValueFactory.fromAnyRef(dstRootCa))
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
