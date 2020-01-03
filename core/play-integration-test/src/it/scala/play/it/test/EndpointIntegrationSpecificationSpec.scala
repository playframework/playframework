/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import okhttp3.Protocol
import okhttp3.Response
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test.PlaySpecification

/**
 * Tests that the [[EndpointIntegrationSpecification]] works properly.
 */
class EndpointIntegrationSpecificationSpec
    extends PlaySpecification
    with EndpointIntegrationSpecification
    with OkHttpEndpointSupport {
  "Endpoints" should {
    "respond with the highest supported HTTP protocol" in {
      withResult(Results.Ok("Hello")).withAllOkHttpEndpoints { okEndpoint: OkHttpEndpoint =>
        val response: Response = okEndpoint.call("/")
        val protocol           = response.protocol
        if (okEndpoint.endpoint.protocols.contains(HTTP_2_0)) {
          protocol must_== Protocol.HTTP_2
        } else if (okEndpoint.endpoint.protocols.contains(HTTP_1_1)) {
          protocol must_== Protocol.HTTP_1_1
        } else {
          ko("All endpoints should support at least HTTP/1.1")
        }
        response.body.string must_== "Hello"
      }
    }
    "respond with the correct server attribute" in withAction { Action: DefaultActionBuilder =>
      Action { request: Request[_] =>
        Results.Ok(request.attrs.get(RequestAttrKey.Server).toString)
      }
    }.withAllOkHttpEndpoints { okHttpEndpoint: OkHttpEndpoint =>
      val response: Response = okHttpEndpoint.call("/")
      response.body.string must_== okHttpEndpoint.endpoint.serverAttribute.toString
    }
  }
}
