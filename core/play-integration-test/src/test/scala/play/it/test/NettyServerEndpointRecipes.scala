/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import scala.util.Properties

import play.api.http.HttpProtocol
import play.api.test.HttpServerEndpointRecipe
import play.api.test.HttpsServerEndpointRecipe
import play.api.test.ServerEndpointRecipe
import play.api.Configuration
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
  ) ++ Seq(
    Netty11Encrypted,
  ).filter(_ => !Properties.isJavaAtLeast(21)) // because of https://github.com/lightbend/ssl-config/issues/367
}
