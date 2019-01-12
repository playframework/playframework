/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc._
import play.api.test._
import play.it.test.{ EndpointIntegrationSpecification, OkHttpEndpointSupport }

/**
 * Specs for the "secure" flag on requests
 */
class SecureFlagSpec extends PlaySpecification
  with EndpointIntegrationSpecification with OkHttpEndpointSupport with ApplicationFactories {

  /** An ApplicationFactory with a single action that returns the request's `secure` flag. */
  val secureFlagAppFactory: ApplicationFactory = withAction { actionBuilder =>
    actionBuilder { request: Request[_] =>
      Results.Ok(request.secure.toString)
    }
  }

  "Play https server" should {
    "show that by default requests are secure only if the protocol is secure" in secureFlagAppFactory.withAllOkHttpEndpoints { okep: OkHttpEndpoint =>
      val response = okep.call("/")
      response.body.string must ===((okep.endpoint.scheme == "https").toString)
    }
    "show that requests are secure if X_FORWARDED_PROTO is https" in secureFlagAppFactory.withAllOkHttpEndpoints { okep: OkHttpEndpoint =>
      val request = okep.requestBuilder("/")
        .addHeader(X_FORWARDED_PROTO, "https")
        .addHeader(X_FORWARDED_FOR, "127.0.0.1")
        .build
      val response = okep.client.newCall(request).execute()
      response.body.string must ===("true")
    }
    "show that requests are insecure if X_FORWARDED_PROTO is http" in secureFlagAppFactory.withAllOkHttpEndpoints { okep: OkHttpEndpoint =>
      val request = okep.requestBuilder("/")
        .addHeader(X_FORWARDED_PROTO, "http")
        .addHeader(X_FORWARDED_FOR, "127.0.0.1")
        .build
      val response = okep.client.newCall(request).execute()
      response.body.string must ===("false")
    }
  }

}
