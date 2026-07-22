/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc.request.ClientCertificateSource
import play.api.mvc.Results
import play.api.test.ApplicationFactories
import play.api.test.PlaySpecification
import play.api.test.ServerEndpointRecipe
import play.it.test.EndpointIntegrationSpecification
import play.it.test.NettyServerEndpointRecipes
import play.it.test.OkHttpEndpointSupport
import play.it.test.PekkoHttpServerEndpointRecipes

class DirectClientCertificateSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport
    with ApplicationFactories {

  private val mutualTlsEndpoints: Seq[ServerEndpointRecipe] = Seq(
    NettyServerEndpointRecipes.Netty11Encrypted,
    PekkoHttpServerEndpointRecipes.PekkoHttp11Encrypted
  ).map(
    _.withExtraServerConfiguration(
      Map("play.server.https.needClientAuth" -> true)
    )
  )

  private val application = withAction { Action =>
    Action { request =>
      val directCertificateMatchesTransport = for {
        clientCertificate <- request.clientCertificate
        transportTls      <- request.transport.tls
      } yield {
        val selectedCertificates  = clientCertificate.certificates.map(_.getEncoded.toSeq)
        val transportCertificates = transportTls.peerCertificates.map(_.getEncoded.toSeq)
        clientCertificate.source == ClientCertificateSource.DirectTransport &&
        selectedCertificates == transportCertificates
      }

      Results.Ok(
        (directCertificateMatchesTransport.contains(true) && request.xForwardedClientCertificates.isEmpty).toString
      )
    }
  }

  "Play HTTPS servers" should {
    "expose a real mutual-TLS client certificate as direct transport metadata" in application
      .withOkHttpEndpoints(mutualTlsEndpoints) { endpoint =>
        val response = endpoint.call("/")

        response.code must_== 200
        response.body.string must_== "true"
      }
  }
}
