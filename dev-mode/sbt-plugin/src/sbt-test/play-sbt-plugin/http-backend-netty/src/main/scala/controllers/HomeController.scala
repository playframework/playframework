/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import jakarta.inject._

import org.apache.pekko.stream.scaladsl.BroadcastHub
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.scaladsl.Keep
import org.apache.pekko.stream.scaladsl.MergeHub
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import play.api.http.websocket.CloseMessage
import play.api.http.websocket.Message
import play.api.mvc._
@Singleton
class HomeController @Inject() (cc: ControllerComponents)(implicit mat: Materializer) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok("Successful response.")
  }

  // like a chat room: many clients -> merge hub -> broadcasthub -> many clients
  // makes it easy to make two websockets communicate with each other
  private val (chatSink, chatSource) = {
    // Don't log MergeHub$ProducerFailed as error if the client disconnects.
    // recoverWithRetries -1 is essentially "recoverWith"
    val source = MergeHub
      .source[String]
      .log("source") // See logback.xml (-> logger "org.apache.pekko.stream.Materializer")
      .recoverWithRetries(-1, { case _: Exception => Source.empty })

    val sink = BroadcastHub.sink[String]

    source.toMat(sink)(Keep.both).run()
  }

  // WebSocket that sends out messages that have been put into chatSink
  def websocketFeedback: WebSocket =
    WebSocket.accept[String, String](rh => Flow.fromSinkAndSource(Sink.ignore, chatSource))

  def websocket: WebSocket = WebSocket.accept[Message, Message](rh =>
    Flow.fromSinkAndSource(
      Sink.foreach(_ match {
        // When the client closes this WebSocket, send the status code
        // that we received from the client to the feedback-websocket
        case CloseMessage(statusCode, _) => Source.single(statusCode.map(_.toString).getOrElse("")).runWith(chatSink)
        case _                           =>
      }),
      Source.maybe[Message]
    ) // Keep connection open
  )
}
