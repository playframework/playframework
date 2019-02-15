/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import play.api.mvc._
import play.api.test._
import play.it.test.{ EndpointIntegrationSpecification, OkHttpEndpointSupport }

class FormFieldOrderSpec extends PlaySpecification
  with EndpointIntegrationSpecification with OkHttpEndpointSupport with ApplicationFactories {

  "Form URL Decoding " should {

    val urlEncoded = "One=one&Two=two&Three=three&Four=four&Five=five&Six=six&Seven=seven"
    val contentType = "application/x-www-form-urlencoded"

    val fakeAppFactory: ApplicationFactory = withAction { actionBuilder =>
      actionBuilder { request: Request[AnyContent] =>
        // Check precondition. This needs to be an x-www-form-urlencoded request body
        request.contentType must beSome(contentType)
        // The following just ingests the request body and converts it to a sequence of strings of the form name=value
        val pairs: Seq[String] = {
          request.body.asFormUrlEncoded map {
            params: Map[String, Seq[String]] =>
              {
                for ((key: String, value: Seq[String]) <- params) yield key + "=" + value.mkString
              }.toSeq
          }
        }.getOrElse(Seq.empty[String])
        // And now this just puts it all back into one string separated by & to reincarnate, hopefully, the
        // original url_encoded string
        val reencoded = pairs.mkString("&")
        // Return the re-encoded body as the result body for comparison below
        Results.Ok(reencoded)
      }
    }

    "preserve form field order" in fakeAppFactory.withAllOkHttpEndpoints { okep: OkHttpEndpoint =>
      val request = new okhttp3.Request.Builder()
        .url(okep.endpoint.pathUrl("/"))
        .post(okhttp3.RequestBody.create(okhttp3.MediaType.parse(contentType), urlEncoded))
        .build()
      val response = okep.client.newCall(request).execute()
      response.code must equalTo(OK)
      // Above the response to the request caused the body to be reconstituted as the url_encoded string.
      // Validate that this is in fact the case, which is the point of this test.
      response.body.string must equalTo(urlEncoded)
    }
  }
}
