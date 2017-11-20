/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.it.test

import okhttp3.{ Protocol, Response }
import play.api.BuiltInComponents
import play.api.mvc._
import play.api.mvc.request.RequestAttrKey
import play.api.test.PlaySpecification

/**
 * Tests that the [[EndpointIntegrationSpecification]] works properly.
 */
class EndpointIntegrationSpecificationSpec extends PlaySpecification with EndpointIntegrationSpecification with OkHttpEndpointSupport {

  "Endpoints" should {
    "respond with the highest supported HTTP protocol" in {
      serveOk("Hello").useOkHttp.forEndpoints { okEndpoint: OkHttpEndpoint =>
        val response: Response = okEndpoint.call("/")
        val protocol = response.protocol
        if (okEndpoint.endpoint.recipe.supportsHttp2) {
          protocol must_== Protocol.HTTP_2
        } else if (okEndpoint.endpoint.recipe.supportsHttp11) {
          protocol must_== Protocol.HTTP_1_1
        } else {
          ko("All endpoints should support at least HTTP/1.1")
        }
        response.body.string must_== "Hello"
      }
    }
    "respond with the correct server attribute" in serveAction { components: BuiltInComponents =>
      components.defaultActionBuilder { request: Request[_] =>
        Results.Ok(request.attrs.get(RequestAttrKey.Server).toString)
      }
    }.useOkHttp.forEndpoints { okHttpEndpoint: OkHttpEndpoint =>
      val response: Response = okHttpEndpoint.call("/")
      response.body.string must_== okHttpEndpoint.endpoint.recipe.serverAttr.toString
    }
  }
}