/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc.Results
import play.api.test.ApplicationFactories
import play.api.test.Helpers.BAD_REQUEST
import play.api.test.PlaySpecification
import play.it.test.EndpointIntegrationSpecification
import play.it.test.NettyServerEndpointRecipes
import play.it.test.PekkoHttpServerEndpointRecipes

class AbsoluteTargetEndpointSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with ApplicationFactories {

  "An absolute request target" should {
    "reject an HTTP scheme received over direct TLS" in {
      withResult(Results.Ok("reached action")).withEndpoints(
        Seq(
          NettyServerEndpointRecipes.Netty11Encrypted,
          PekkoHttpServerEndpointRecipes.PekkoHttp11Encrypted
        )
      ) { endpoint =>
        val Seq(response) = BasicHttpClient.makeRequests(endpoint.port, secure = true)(
          BasicRequest("GET", "http://localhost/path", "HTTP/1.1", Map.empty, "")
        )

        response.status must_== BAD_REQUEST
      }
    }
  }
}
