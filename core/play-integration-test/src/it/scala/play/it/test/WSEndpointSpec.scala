/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.test.PlaySpecification

/**
 * Tests that [[OkHttpEndpointSupport]] works properly.
 */
class WSEndpointSpec extends PlaySpecification with EndpointIntegrationSpecification with WSEndpointSupport {
  "WSEndpoint" should {
    "make a request and get a response" in {
      withResult(Results.Ok("Hello")).withAllWSEndpoints { endpointClient: WSEndpoint =>
        val response: WSResponse = endpointClient.makeRequest("/")
        response.body must_== "Hello"
      }
    }
  }
}
