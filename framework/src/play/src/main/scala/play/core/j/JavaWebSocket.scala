/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core.j

import java.util.concurrent.{ CompletableFuture, CompletionStage }

import akka.actor.Status
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{ Source, Flow, Sink }
import play.api.libs.streams.ActorFlow
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc._
import play.mvc.Http.{ Context => JContext }
import play.mvc.{ WebSocket => JWebSocket, LegacyWebSocket }
import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters

import com.fasterxml.jackson.databind.JsonNode

import play.core.Execution.Implicits.internalContext

/**
 * handles a scala websocket in a Java Context
 */
object JavaWebSocket extends JavaHelpers {

  def webSocketWrapper[A](retrieveWebSocket: => CompletionStage[LegacyWebSocket[A]])(implicit transformer: MessageFlowTransformer[A, A]): WebSocket = WebSocket { request =>

    val javaContext = createJavaContext(request)

    val javaWebSocket = try {
      JContext.current.set(javaContext)
      FutureConverters.toScala(retrieveWebSocket)
    } finally {
      JContext.current.remove()
    }

    javaWebSocket.map { jws =>
      val reject = Option(jws.rejectWith())
      reject.map { result =>

        Left(createResult(javaContext, result))

      } getOrElse {
        val current = play.api.Play.privateMaybeApplication.get
        implicit val system = current.actorSystem
        implicit val mat = current.materializer

        Right(

          if (jws.isActor) {
            transformer.transform(ActorFlow.actorRef(jws.actorProps))
          } else {

            val socketIn = new JWebSocket.In[A]

            val sink = Flow[A].map { msg =>
              socketIn.callbacks.asScala.foreach(_.accept(msg))
            }.to(Sink.onComplete { _ =>
              socketIn.closeCallbacks.asScala.foreach(_.run())
            })

            val source = Source.actorRef[A](256, OverflowStrategy.dropNew).mapMaterializedValue { actor =>
              val socketOut = new JWebSocket.Out[A] {
                def write(frame: A) = {
                  actor ! frame
                }
                def close() = {
                  actor ! Status.Success(())
                }
              }

              jws.onReady(socketIn, socketOut)
            }

            transformer.transform(Flow.fromSinkAndSource(sink, source))
          }
        )
      }
    }
  }

  // -- Bytes

  def ofBytes(retrieveWebSocket: => LegacyWebSocket[Array[Byte]]): WebSocket =
    webSocketWrapper[Array[Byte]](CompletableFuture.completedFuture(retrieveWebSocket))

  def promiseOfBytes(retrieveWebSocket: => CompletionStage[LegacyWebSocket[Array[Byte]]]): WebSocket =
    webSocketWrapper[Array[Byte]](retrieveWebSocket)

  // -- String

  def ofString(retrieveWebSocket: => LegacyWebSocket[String]): WebSocket =
    webSocketWrapper[String](CompletableFuture.completedFuture(retrieveWebSocket))

  def promiseOfString(retrieveWebSocket: => CompletionStage[LegacyWebSocket[String]]): WebSocket =
    webSocketWrapper[String](retrieveWebSocket)

  // -- Json (JsonNode)

  implicit val jsonFrame = MessageFlowTransformer.stringMessageFlowTransformer.map(
    play.libs.Json.parse, play.libs.Json.stringify
  )

  def ofJson(retrieveWebSocket: => LegacyWebSocket[JsonNode]): WebSocket =
    webSocketWrapper[JsonNode](CompletableFuture.completedFuture(retrieveWebSocket))

  def promiseOfJson(retrieveWebSocket: => CompletionStage[LegacyWebSocket[JsonNode]]): WebSocket =
    webSocketWrapper[JsonNode](retrieveWebSocket)
}
