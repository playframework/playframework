/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.openid

import org.specs2.mock.Mockito
import play.api.http.HeaderNames
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

  request.get() returns Future.successful(response.asInstanceOf[request.Response])
  request.post(anyString)(any[BodyWritable[String]]) returns Future.successful(response.asInstanceOf[request.Response])
  request.post(any[Map[String, Seq[String]]])(any[BodyWritable[Map[String, Seq[String]]]]) returns Future.successful(response.asInstanceOf[request.Response])

  def url(url: String): WSRequest = {
    urls += url
    request
  }

  def underlying[T]: T = this.asInstanceOf[T]

  def close() = ()
}
