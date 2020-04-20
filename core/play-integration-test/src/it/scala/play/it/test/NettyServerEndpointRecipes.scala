/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api.Configuration
import play.api.http.HttpProtocol
import play.api.test.HttpServerEndpointRecipe
import play.api.test.HttpsServerEndpointRecipe
import play.api.test.ServerEndpointRecipe
import play.core.server.NettyServer

object NettyServerEndpointRecipes {
  val Netty11Plaintext = new HttpServerEndpointRecipe(
    "Netty HTTP/1.1 (plaintext)",
    NettyServer.provider,
    Configuration.empty,
    Set(HttpProtocol.HTTP_1_0, HttpProtocol.HTTP_1_1),
    Option("netty")
  )

  val Netty11Encrypted = new HttpsServerEndpointRecipe(
    "Netty HTTP/1.1 (encrypted)",
    NettyServer.provider,
    Configuration.empty,
    Set(HttpProtocol.HTTP_1_0, HttpProtocol.HTTP_1_1),
    Option("netty")
  )

  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    Netty11Plaintext,
    Netty11Encrypted
  )
}
