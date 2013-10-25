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
import scala.collection.immutable.ListMap


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

    val response_text = "preserve form field order"
    val urlencoded = "One=one&Two=two&Three=three&Four=four&Five=five&Six=six&Seven=seven"
    val content_type = "application/x-www-form-urlencoded"
    var saved_body : String = null

    "preserve form field order" in withServer( Action { request: Request[AnyContent] =>
      val as_text = request.body.asText.getOrElse("")
      val pairs: Seq[String] = { request.body.asFormUrlEncoded map { params: ListMap[String,Seq[String]] =>
        {for ( (key:String,value:Seq[String]) <- request.body.asFormUrlEncoded.get ) yield key + "=" + value
          .mkString }.toSeq
      }}.getOrElse(Seq.empty[String])
      val reencoded = pairs.mkString("&")
      saved_body = reencoded
      request.headers.get("Content-Type").getOrElse("") must equalTo(content_type)
      Results.Ok(response_text)
    }) { port =>
      val request : WSRequestHolder =
        WS.url("http://localhost:" + port).
           withHeaders("Content-Type" -> content_type).
           withRequestTimeout(10000)

      val future = request.post(urlencoded)

      val response = await(future, 10000)
      response.status must equalTo(OK)
      response.body must equalTo(response_text)
      saved_body must equalTo(urlencoded)
    }
  }
}
