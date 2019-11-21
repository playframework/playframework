package play.it.test

import play.api.Configuration
import play.api.test.{HttpServerEndpointRecipe, HttpsServerEndpointRecipe, ServerEndpointRecipe}
import play.core.server.NettyServer

object NettyServerEndpointRecipes {

  val Netty11Plaintext = new HttpServerEndpointRecipe(
    "Netty HTTP/1.1 (plaintext)",
    NettyServer.provider,
    Configuration.empty,
    Set("1.0", "1.1"),
    Option("netty")
  )
  val Netty11Encrypted = new HttpsServerEndpointRecipe(
    "Netty HTTP/1.1 (encrypted)",
    NettyServer.provider,
    Configuration.empty,
    Set("1.0", "1.1"),
    Option("netty")
  )

  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted
  )
}
