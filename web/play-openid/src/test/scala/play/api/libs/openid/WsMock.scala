/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.openid

import scala.concurrent.Future

import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.when
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws._

class WSMock extends WSClient {
  val request  = mock(classOf[WSRequest])
  val response = mock(classOf[WSResponse])

  val urls: collection.mutable.Buffer[String] = new collection.mutable.ArrayBuffer[String]()

  when(response.status).thenReturn(OK)
  when(response.header(HeaderNames.CONTENT_TYPE)).thenReturn(Some("text/html;charset=UTF-8"))
  when(response.body).thenReturn("")

  when(request.get()).thenReturn(Future.successful(response.asInstanceOf[request.Response]))
  when(request.post(anyString)(using any[BodyWritable[String]]))
    .thenReturn(Future.successful(response.asInstanceOf[request.Response]))
  when(request.post(any[Map[String, Seq[String]]])(using any[BodyWritable[Map[String, Seq[String]]]]))
    .thenReturn(Future.successful(response.asInstanceOf[request.Response]))

  def url(url: String): WSRequest = {
    urls += url
    request
  }

  def underlying[T]: T = this.asInstanceOf[T]

  def close() = ()
}
