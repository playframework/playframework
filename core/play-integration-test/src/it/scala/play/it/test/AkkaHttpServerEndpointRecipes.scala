/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api.Configuration
import play.api.http.HttpProtocol
import play.api.test.HttpServerEndpointRecipe
import play.api.test.HttpsServerEndpointRecipe
import play.api.test.ServerEndpointRecipe
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
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1),
    None
  )

  val AkkaHttp11Encrypted = new HttpsServerEndpointRecipe(
    "Akka HTTP HTTP/1.1 (encrypted)",
    AkkaHttpServer.provider,
    http2Conf(enabled = false),
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1),
    None
  )

  val AkkaHttp20Plaintext = new HttpServerEndpointRecipe(
    "Akka HTTP HTTP/2 (plaintext)",
    AkkaHttpServer.provider,
    http2Conf(enabled = true, alwaysForInsecure = true),
    Set(HttpProtocol.HTTP_2_0),
    None
  )

  val AkkaHttp20Encrypted = new HttpsServerEndpointRecipe(
    "Akka HTTP HTTP/2 (encrypted)",
    AkkaHttpServer.provider,
    http2Conf(enabled = true),
    Set(HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_1_1, HttpProtocol.HTTP_2_0),
    None
  )

  val AllRecipes: Seq[ServerEndpointRecipe] = Seq(
    AkkaHttp11Plaintext,
    AkkaHttp11Encrypted,
    AkkaHttp20Encrypted
  )

  val AllRecipesIncludingExperimental: Seq[ServerEndpointRecipe] = AllRecipes :+ AkkaHttp20Plaintext
}
