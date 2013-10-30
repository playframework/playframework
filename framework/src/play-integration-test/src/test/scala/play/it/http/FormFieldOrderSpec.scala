/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.it.http

import play.api.mvc._
import play.api.test._
import play.api.test.TestServer
import play.api.libs.ws.WS
import play.api.libs.ws.WS.WSRequestHolder
import play.api.test.FakeApplication


object FormFieldOrderSpec extends PlaySpecification {

  "Play' form URL Decoding " should {

    def withServer[T](action: EssentialAction)(block: Port => T) = {
      val port = testServerPort
      running(TestServer(port, FakeApplication(
        withRoutes = {
          case _ => action
        }
      ))) {
        block(port)
      }
    }

    val url_encoded = "One=one&Two=two&Three=three&Four=four&Five=five&Six=six&Seven=seven"
    val content_type = "application/x-www-form-urlencoded"

    "preserve form field order" in withServer( Action { request: Request[AnyContent] =>
      // Check precondition. This needs to be an x-www-form-urlencoded request body
      request.headers.get("Content-Type").getOrElse("") must equalTo(content_type)
      // The following just ingests the request body and converts it to a sequnce of strings of the form name=value
      val pairs: Seq[String] = { request.body.asFormUrlEncoded map { params: Map[String,Seq[String]] =>
        {for ( (key:String,value:Seq[String]) <- params ) yield key + "=" + value.mkString }.toSeq
      }}.getOrElse(Seq.empty[String])
      // And now this just puts it all back into one string separated by & to reincarnate, hopefully, the
      // original url_encoded string
      val reencoded = pairs.mkString("&")
      // Return the re-encoded body as the result body for comparison below
      Results.Ok(reencoded)
    }) { port =>
      val request : WSRequestHolder =
        WS.url("http://localhost:" + port).
           withHeaders("Content-Type" -> content_type).
           withRequestTimeout(10000)

      val future = request.post(url_encoded)

      val response = await(future, 10000)
      response.status must equalTo(OK)
      // Above the response to the request caused the body to be reconstituted as the url_encoded string.
      // Validate that this is in fact the case, which is the point of this test.
      response.body must equalTo(url_encoded)
    }
  }
}
