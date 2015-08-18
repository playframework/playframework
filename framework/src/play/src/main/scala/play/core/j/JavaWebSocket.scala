/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import akka.actor.Status
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Keep, Source, Flow, Sink }
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.mvc.Http.{ Context => JContext }
import play.mvc.{ WebSocket => JWebSocket }
import play.libs.F.{ Promise => JPromise }
import scala.collection.JavaConverters._

import scala.concurrent.Future
import com.fasterxml.jackson.databind.JsonNode
import play.api.libs.concurrent.Akka

import play.api.Play.current
import play.core.Execution.Implicits.internalContext

/**
 * handles a scala websocket in a Java Context
 */
object JavaWebSocket extends JavaHelpers {

  def webSocketWrapper[A](retrieveWebSocket: => Future[JWebSocket[A]])(implicit transformer: MessageFlowTransformer[A, A]): WebSocket = WebSocket { request =>

    val javaContext = createJavaContext(request)

    val javaWebSocket = try {
      JContext.current.set(javaContext)
      retrieveWebSocket
    } finally {
      JContext.current.remove()
    }

    javaWebSocket.map { jws =>
      val reject = Option(jws.rejectWith())
      reject.map { result =>

        Left(createResult(javaContext, result))

      } getOrElse {

        implicit val system = Akka.system
        implicit val mat = current.materializer

        Right(

          if (jws.isActor) {
            transformer.transform(ActorFlow.actorRef(jws.actorProps))
          } else {

            val socketIn = new play.mvc.WebSocket.In[A]

            val sink = Flow[A].map { msg =>
              socketIn.callbacks.asScala.foreach(_.accept(msg))
            }.to(Sink.onComplete { _ =>
              socketIn.closeCallbacks.asScala.foreach(_.run())
            })

            val source = Source.actorRef[A](256, OverflowStrategy.dropNew).mapMaterializedValue { actor =>
              val socketOut = new play.mvc.WebSocket.Out[A] {
                def write(frame: A) = {
                  actor ! frame
                }
                def close() = {
                  actor ! Status.Success(())
                }
              }

              jws.onReady(socketIn, socketOut)
            }

            transformer.transform(Flow.wrap(sink, source)(Keep.none))
          }
        )
      }
    }
  }

  // -- Bytes

  def ofBytes(retrieveWebSocket: => JWebSocket[Array[Byte]]): WebSocket =
    webSocketWrapper[Array[Byte]](Future.successful(retrieveWebSocket))

  def promiseOfBytes(retrieveWebSocket: => JPromise[JWebSocket[Array[Byte]]]): WebSocket =
    webSocketWrapper[Array[Byte]](retrieveWebSocket.wrapped())

  // -- String

  def ofString(retrieveWebSocket: => JWebSocket[String]): WebSocket =
    webSocketWrapper[String](Future.successful(retrieveWebSocket))

  def promiseOfString(retrieveWebSocket: => JPromise[JWebSocket[String]]): WebSocket =
    webSocketWrapper[String](retrieveWebSocket.wrapped())

  // -- Json (JsonNode)

  implicit val jsonFrame = MessageFlowTransformer.stringMessageFlowTransformer.map(
    play.libs.Json.parse, play.libs.Json.stringify
  )

  def ofJson(retrieveWebSocket: => JWebSocket[JsonNode]): WebSocket =
    webSocketWrapper[JsonNode](Future.successful(retrieveWebSocket))

  def promiseOfJson(retrieveWebSocket: => JPromise[JWebSocket[JsonNode]]): WebSocket =
    webSocketWrapper[JsonNode](retrieveWebSocket.wrapped())
}
