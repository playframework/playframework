/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api.http.HttpProtocol
import play.api.test.HttpServerEndpointRecipe
import play.api.test.HttpsServerEndpointRecipe
import play.api.test.ServerEndpointRecipe
import play.api.Configuration
import play.core.server.PekkoHttpServer

object PekkoHttpServerEndpointRecipes {
  private def http2Conf(enabled: Boolean, alwaysForInsecure: Boolean = false): Configuration = Configuration(
    "play.server.akka.http2.enabled"           -> enabled,
    "play.server.akka.http2.alwaysForInsecure" -> alwaysForInsecure
  )

  val PekkoHttp11Plaintext = new HttpServerEndpointRecipe(
    "Pekko HTTP HTTP/1.1 (plaintext)",
    PekkoHttpServer.provider,
    http2Conf(enabled = false),
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1),
    None
  )

  val PekkoHttp11Encrypted = new HttpsServerEndpointRecipe(
    "Pekko HTTP HTTP/1.1 (encrypted)",
    PekkoHttpServer.provider,
    http2Conf(enabled = false),
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1),
    None
  )

  val PekkoHttp20Plaintext = new HttpServerEndpointRecipe(
    "Pekko HTTP HTTP/2 (plaintext)",
    PekkoHttpServer.provider,
    http2Conf(enabled = true, alwaysForInsecure = true),
    Set(HttpProtocol.HTTP_2_0),
    None
  )

  val PekkoHttp20Encrypted = new HttpsServerEndpointRecipe(
    "Pekko HTTP HTTP/2 (encrypted)",
    PekkoHttpServer.provider,
    http2Conf(enabled = true),
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_2_0),
    None
  )

  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    PekkoHttp11Plaintext,
    PekkoHttp11Encrypted,
    PekkoHttp20Encrypted
  )

  val AllRecipesIncludingExperimental: Seq[ServerEndpointRecipe] = AllRecipes :+ PekkoHttp20Plaintext
}
