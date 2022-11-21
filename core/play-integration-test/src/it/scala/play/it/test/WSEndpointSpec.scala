/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import play.api.libs.ws._
import play.api.mvc._
import play.api.test.PlaySpecification

/**
 * Tests that [[OkHttpEndpointSupport]] works properly.
 */
class WSEndpointSpec extends PlaySpecification with EndpointIntegrationSpecification with WSEndpointSupport {
  "WSEndpoint" should {
    "make a request and get a response" in {
      withResult(Results.Ok("Hello")).withAllWSEndpoints { (endpointClient: WSEndpoint) =>
        val response: WSResponse = endpointClient.makeRequest("/")
        response.body[String] must_== "Hello"
      }
    }
  }
}
