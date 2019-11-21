package play.it.test

import akka.annotation.ApiMayChange
import play.api.Configuration
import play.api.test.{HttpServerEndpointRecipe, HttpsServerEndpointRecipe, ServerEndpointRecipe}
import play.core.server.AkkaHttpServer

object AkkaHttpServerEndpointRecipes {

  private def http2Conf(enabled: Boolean, alwaysForInsecure: Boolean = false): Configuration = Configuration(
    "play.server.akka.http2.enabled"           -> enabled,
    "play.server.akka.http2.alwaysForInsecure" -> alwaysForInsecure
  )

  val AkkaHttp11Plaintext = new HttpServerEndpointRecipe(
    "Akka HTTP HTTP/1.1 (plaintext)",
    AkkaHttpServer.provider,
    http2Conf(enabled = false),
    Set("1.0", "1.1"),
    None
  )
  val AkkaHttp11Encrypted = new HttpsServerEndpointRecipe(
    "Akka HTTP HTTP/1.1 (encrypted)",
    AkkaHttpServer.provider,
    http2Conf(enabled = false),
    Set("1.0", "1.1"),
    None
  )
  @ApiMayChange
  val AkkaHttp20Plaintext = new HttpServerEndpointRecipe(
    "Akka HTTP HTTP/2 (plaintext)",
    AkkaHttpServer.provider,
    http2Conf(enabled = true, alwaysForInsecure = true),
    Set("2"),
    None
  )
  val AkkaHttp20Encrypted = new HttpsServerEndpointRecipe(
    "Akka HTTP HTTP/2 (encrypted)",
    AkkaHttpServer.provider,
    http2Conf(enabled = true),
    Set("1.0", "1.1", "2"),
    None
  )

  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )

  val AllRecipesIncludingExperimental: Seq[ServerEndpointRecipe] = AllRecipes :+ AkkaHttp20Plaintext
}
