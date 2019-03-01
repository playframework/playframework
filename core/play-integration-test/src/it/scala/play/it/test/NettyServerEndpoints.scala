package play.it.test

import play.api.Configuration
import play.api.test.HttpServerEndpointRecipe
import play.api.test.HttpsServerEndpointRecipe
import play.core.server.NettyServer

object NettyServerEndpoints {

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

  val NettyEndpoints = Seq(
    Netty11Plaintext,
    Netty11Encrypted
  )

}
