/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import okhttp3.Response
import play.api.test.PlaySpecification

/**
 * Tests that [[OkHttpEndpointSupport]] works properly.
 */
class OkHttpEndpointSpec extends PlaySpecification with EndpointIntegrationSpecification with OkHttpEndpointSupport {

  "OkHttpEndpoint" should {
    "make a request and get a response" in {
      serveOk("Hello").useOkHttp.forEndpoints { okEndpoint: OkHttpEndpoint =>
        val response: Response = okEndpoint.call("/")
        response.body.string must_== "Hello"
      }
    }
  }
}