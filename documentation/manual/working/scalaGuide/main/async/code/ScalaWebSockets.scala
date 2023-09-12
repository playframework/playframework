/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.async.websockets

import scala.concurrent.Future
import scala.concurrent.Promise

import play.api.http.websocket.Message
import play.api.http.websocket.TextMessage
import play.api.test._

class ScalaWebSockets extends PlaySpecification {
  import java.io.Closeable

  import akka.stream.scaladsl._
  import akka.stream.Materializer
  import play.api.libs.json.Json
  import play.api.libs.streams.ActorFlow
  import play.api.mvc.Result
  import play.api.mvc.WebSocket

  "Scala WebSockets" should {
    def runWebSocket[In, Out](webSocket: WebSocket, in: Source[Message, _], expectOut: Int)(
        implicit mat: Materializer
    ): Either[Result, List[Message]] = {
      await(webSocket(FakeRequest())).map { flow =>
        // When running in the real world, if the flow cancels upstream, Play's WebSocket protocol implementation will
        // handle this and close the WebSocket, but here, that won't happen, so we redeem the future when we receive
        // enough.
        val promise = Promise[List[Message]]()
        if (expectOut == 0) promise.success(Nil)
        val flowResult = in
          .via(flow)
          .runWith(Sink.fold[(List[Message], Int), Message]((Nil, expectOut)) { (state, out) =>
            val (result, remaining) = state
            if (remaining == 1) {
              promise.success(result :+ out)
            }
            (result :+ out, remaining - 1)
          })
        import mat.executionContext
        await(Future.firstCompletedOf(Seq(promise.future, flowResult.map(_._1))))
      }
    }

    "support actors" in {
      import akka.actor._

      "allow creating a simple echoing actor" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller1.Application]
          runWebSocket(controller.socket, Source.single(TextMessage("foo")), 1) must beRight.like {
            case list => list must_== List(TextMessage("I received your message: foo"))
          }
        }
      }

      "allow cleaning up" in new WithApplication() {
        override def running() = {
          val closed = Promise[Boolean]()
          val someResource = new Closeable() {
            def close() = closed.success(true)
          }
          class MyActor extends Actor {
            def receive: PartialFunction[Any, Nothing] = PartialFunction.empty

            // #actor-post-stop
            override def postStop() = {
              someResource.close()
            }
            // #actor-post-stop
          }

          implicit def actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

          runWebSocket(
            WebSocket.accept[String, String](req => ActorFlow.actorRef(out => Props(new MyActor))),
            Source.empty,
            0
          ) must beRight[List[Message]]
          await(closed.future) must_== true
        }
      }

      "allow closing the WebSocket" in new WithApplication() {
        override def running() = {
          class MyActor extends Actor {
            def receive: PartialFunction[Any, Nothing] = PartialFunction.empty

            // #actor-stop

            import akka.actor.PoisonPill

            self ! PoisonPill
            // #actor-stop
          }

          implicit def actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

          runWebSocket(
            WebSocket.accept[String, String](req => ActorFlow.actorRef(out => Props(new MyActor))),
            Source.maybe,
            0
          ) must beRight[List[Message]]
        }
      }

      "allow rejecting the WebSocket" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller2.Application]
          runWebSocket(controller.socket, Source.empty, 0) must beLeft.which { result =>
            result.header.status must_== FORBIDDEN
          }
        }
      }

      "allow creating a json actor" in new WithApplication() {
        override def running() = {
          val json       = Json.obj("foo" -> "bar")
          val controller = app.injector.instanceOf[Controller4.Application]
          runWebSocket(controller.socket, Source.single(TextMessage(Json.stringify(json))), 1) must beRight.which {
            out =>
              out must_== List(TextMessage(Json.stringify(json)))
          }
        }
      }

      "allow creating a higher level object actor" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller5.Application]
          runWebSocket(
            controller.socket,
            Source.single(TextMessage(Json.stringify(Json.toJson(Controller5.InEvent("blah"))))),
            1
          ) must beRight.which { out =>
            out must_== List(TextMessage(Json.stringify(Json.toJson(Controller5.OutEvent("blah")))))
          }
        }
      }
    }

    "support iteratees" in {
      "iteratee1" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller6]
          runWebSocket(controller.socket, Source.empty, 1) must beRight.which { out =>
            out must_== List(TextMessage("Hello!"))
          }
        }
      }

      "iteratee2" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller7]
          runWebSocket(controller.socket, Source.maybe, 1) must beRight.which { out =>
            out must_== List(TextMessage("Hello!"))
          }
        }
      }

      "iteratee3" in new WithApplication() {
        override def running() = {
          val controller = app.injector.instanceOf[Controller8]
          runWebSocket(controller.socket, Source.single(TextMessage("foo")), 1) must beRight.which { out =>
            out must_== List(TextMessage("I received your message: foo"))
          }
        }
      }
    }
  }

  /**
   * The default await timeout.  Override this to change it.
   */
  import scala.concurrent.duration._
  implicit override def defaultAwaitTimeout = 2.seconds
}

object Controller1 {
  // format: off
  import Actor1.MyWebSocketActor
  // format: on

  // #actor-accept
  import javax.inject.Inject

  import akka.actor.ActorSystem
  import akka.stream.Materializer
  import play.api.libs.streams.ActorFlow
  import play.api.mvc._

  class Application @Inject() (cc: ControllerComponents)(implicit system: ActorSystem, mat: Materializer)
      extends AbstractController(cc) {
    def socket = WebSocket.accept[String, String] { request =>
      ActorFlow.actorRef { out => MyWebSocketActor.props(out) }
    }
  }
  // #actor-accept
}

object Actor1 {
  // #example-actor
  import akka.actor._

  object MyWebSocketActor {
    def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  }

  class MyWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case msg: String =>
        out ! ("I received your message: " + msg)
    }
  }
  // #example-actor
}

object Controller2 {
  // format: off
  import Actor1.MyWebSocketActor
  // format: on

  // #actor-try-accept
  import javax.inject.Inject

  import akka.actor.ActorSystem
  import akka.stream.Materializer
  import play.api.libs.streams.ActorFlow
  import play.api.mvc._

  class Application @Inject() (cc: ControllerComponents)(implicit system: ActorSystem, mat: Materializer)
      extends AbstractController(cc) {
    def socket = WebSocket.acceptOrResult[String, String] { request =>
      Future.successful(request.session.get("user") match {
        case None => Left(Forbidden)
        case Some(_) =>
          Right(ActorFlow.actorRef { out => MyWebSocketActor.props(out) })
      })
    }
  }
}
//#actor-try-accept

object Controller4 {
  import akka.actor._

  class MyWebSocketActor(out: ActorRef) extends Actor {
    import play.api.libs.json.JsValue
    def receive = {
      case msg: JsValue =>
        out ! msg
    }
  }

  object MyWebSocketActor {
    def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  }

  // #actor-json
  import javax.inject.Inject

  import akka.actor.ActorSystem
  import akka.stream.Materializer
  import play.api.libs.json._
  import play.api.libs.streams.ActorFlow
  import play.api.mvc._

  class Application @Inject() (cc: ControllerComponents)(implicit system: ActorSystem, mat: Materializer)
      extends AbstractController(cc) {
    def socket = WebSocket.accept[JsValue, JsValue] { request =>
      ActorFlow.actorRef { out => MyWebSocketActor.props(out) }
    }
  }
  // #actor-json
}

object Controller5 {
  case class InEvent(foo: String)
  case class OutEvent(bar: String)

  // #actor-json-formats
  import play.api.libs.json._

  implicit val inEventFormat: Format[InEvent]   = Json.format[InEvent]
  implicit val outEventFormat: Format[OutEvent] = Json.format[OutEvent]
  // #actor-json-formats

  import akka.actor._

  class MyWebSocketActor(out: ActorRef) extends Actor {
    def receive = {
      case InEvent(foo) =>
        out ! OutEvent(foo)
    }
  }

  object MyWebSocketActor {
    def props(out: ActorRef) = Props(new MyWebSocketActor(out))
  }

  // #actor-json-frames
  import play.api.mvc.WebSocket.MessageFlowTransformer

  implicit val messageFlowTransformer: MessageFlowTransformer[InEvent, OutEvent] =
    MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]
  // #actor-json-frames

  // #actor-json-in-out
  import javax.inject.Inject

  import akka.actor.ActorSystem
  import akka.stream.Materializer
  import play.api.libs.streams.ActorFlow
  import play.api.mvc._

  class Application @Inject() (cc: ControllerComponents)(implicit system: ActorSystem, mat: Materializer)
      extends AbstractController(cc) {
    def socket = WebSocket.accept[InEvent, OutEvent] { request =>
      ActorFlow.actorRef { out => MyWebSocketActor.props(out) }
    }
  }
  // #actor-json-in-out
}

class Controller6 {
  // #streams1
  import akka.stream.scaladsl._
  import play.api.mvc._

  def socket = WebSocket.accept[String, String] { request =>
    // Log events to the console
    val in = Sink.foreach[String](println)

    // Send a single 'Hello!' message and then leave the socket open
    val out = Source.single("Hello!").concat(Source.maybe)

    Flow.fromSinkAndSource(in, out)
  }
  // #streams1
}

class Controller7 {
  // #streams2
  import akka.stream.scaladsl._
  import play.api.mvc._

  def socket = WebSocket.accept[String, String] { request =>
    // Just ignore the input
    val in = Sink.ignore

    // Send a single 'Hello!' message and close
    val out = Source.single("Hello!")

    Flow.fromSinkAndSource(in, out)
  }
  // #streams2
}

class Controller8 {
  // #streams3
  import akka.stream.scaladsl._
  import play.api.mvc._

  def socket = WebSocket.accept[String, String] { request =>
    // log the message to stdout and send response back to client
    Flow[String].map { msg =>
      println(msg)
      "I received your message: " + msg
    }
  }
  // #streams3
}
