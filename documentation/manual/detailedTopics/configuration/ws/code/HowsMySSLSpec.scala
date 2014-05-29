package detailedtopics.configuration.ws {

// #context
import play.api.libs.json.JsSuccess
import play.api.libs.ws._
import play.api.libs.ws.ning._
import play.api.libs.ws.ssl.debug.DebugConfiguration
import play.api.test._

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

class HowsMySSLSpec extends PlaySpecification {

  def createClient(rawConfig: play.api.Configuration): WSClient = {
    val classLoader = Thread.currentThread().getContextClassLoader
    val parser = new DefaultWSConfigParser(rawConfig, classLoader)
    val clientConfig = parser.parse()
    clientConfig.ssl.map {
      _.debug.map(new DebugConfiguration().configure)
    }
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
      val rawConfig = play.api.Configuration(ConfigFactory.parseString(
        """
          |ws.ssl.debug=["ssl"]
          |ws.ssl.protocol="TLSv1"
          |ws.ssl.enabledProtocols=["TLSv1"]
        """.stripMargin))

      val client = createClient(rawConfig)
      val response = await(client.url("https://www.howsmyssl.com/a/check").get())(5.seconds)
      response.status must be_==(200)

      val jsonOutput = response.json
      val result = (jsonOutput \ "tls_version").validate[String]
      result must beLike {
        case JsSuccess(value, path) =>
          value must contain("TLS 1.0")
      }
    }
  }
}
// #context

}
