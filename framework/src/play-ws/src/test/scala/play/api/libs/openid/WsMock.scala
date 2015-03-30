/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.openid

import org.specs2.mock.Mockito
import play.api.http.{ ContentTypeOf, Writeable, HeaderNames }
import play.api.libs.ws._
import play.api.http.Status._
import scala.concurrent.Future

class WSMock extends Mockito with WSClient {
  val request = mock[WSRequest]
  val response = mock[WSResponse]

  val urls: collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

  response.status returns OK
  response.header(HeaderNames.CONTENT_TYPE) returns Some("text/html;charset=UTF-8")
  response.body returns ""

  request.get() returns Future.successful(response)
  request.post(anyString)(any[Writeable[String]]) returns Future.successful(response)

  def url(url: String): WSRequest = {
    urls += url
    request
  }

  def underlying[T]: T = this.asInstanceOf[T]

  def close() = ()
}
