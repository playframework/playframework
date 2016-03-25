/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.async.websockets

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl._
import play.api.http.websocket.{ TextMessage, Message }
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.test._
import scala.concurrent.{ Future, Promise }

object ScalaWebSockets extends PlaySpecification {

  import java.io.Closeable
  import play.api.mvc.{Result, WebSocket}
  import play.api.libs.json.Json

  "Scala WebSockets" should {

    def runWebSocket[In, Out](webSocket: WebSocket, in: Source[Message, _], expectOut: Int)(implicit mat: Materializer): Either[Result, List[Message]] = {
      await(webSocket(FakeRequest())).right.map { flow =>

        // When running in the real world, if the flow cancels upstream, Play's WebSocket protocol implementation will
        // handle this and close the WebSocket, but here, that won't happen, so we redeem the future when we receive
        // enough.
        val promise = Promise[List[Message]]()
        if (expectOut == 0) promise.success(Nil)
        val flowResult = in via flow runWith Sink.fold[(List[Message], Int), Message]((Nil, expectOut)) { (state, out) =>
          val (result, remaining) = state
          if (remaining == 1) {
            promise.success(result :+ out)
          }
          (result :+ out, remaining - 1)
        }
        import play.api.libs.iteratee.Execution.Implicits.trampoline
        await(Future.firstCompletedOf(Seq(promise.future, flowResult.map(_._1))))
      }
    }

    "support actors" in {

      import akka.actor._

      "allow creating a simple echoing actor" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller1]
        runWebSocket(controller.socket, Source.single(TextMessage("foo")), 1) must beRight.like {
          case list => list must_== List(TextMessage("I received your message: foo"))
        }
      }

      "allow cleaning up" in new WithApplication() {
        val closed = Promise[Unit]()
        val someResource = new Closeable() {
          def close() = closed.success(())
        }
        class MyActor extends Actor {
          def receive = PartialFunction.empty

          //#actor-post-stop
          override def postStop() = {
            someResource.close()
          }
          //#actor-post-stop
        }

        runWebSocket(
          WebSocket.acceptWithActor[String, String](req => out => Props(new MyActor)), Source.empty, 0
        ) must beRight[List[Message]]
        await(closed.future) must_== ()
      }

      "allow closing the WebSocket" in new WithApplication() {
        class MyActor extends Actor {
          def receive = PartialFunction.empty

          //#actor-stop
          import akka.actor.PoisonPill

          self ! PoisonPill
          //#actor-stop
        }

        runWebSocket(
          WebSocket.acceptWithActor[String, String](req => out => Props(new MyActor)), Source.maybe, 0
        ) must beRight[List[Message]]
      }

      "allow rejecting the WebSocket" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller3]
        runWebSocket(controller.socket, Source.empty, 0) must beLeft.which { result =>
          result.header.status must_== FORBIDDEN
        }
      }

      "allow creating a json actor" in new WithApplication() {
        val json = Json.obj("foo" -> "bar")
        val controller = app.injector.instanceOf[Samples.Controller4]
        runWebSocket(controller.socket, Source.single(TextMessage(Json.stringify(json))), 1) must beRight.which { out =>
          out must_== List(TextMessage(Json.stringify(json)))
        }
      }

      "allow creating a higher level object actor" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller5]
        runWebSocket(
          controller.socket,
          Source.single(TextMessage(Json.stringify(Json.toJson(Samples.InEvent("blah"))))),
          1
        ) must beRight.which { out =>
          out must_== List(TextMessage(Json.stringify(Json.toJson(Samples.OutEvent("blah")))))
        }
      }

    }

    "support iteratees" in {

      "iteratee1" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller6]
        runWebSocket(controller.socket, Source.empty, 1) must beRight.which { out =>
          out must_== List(TextMessage("Hello!"))
        }
      }

      "iteratee2" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller7]
        runWebSocket(controller.socket, Source.maybe, 1) must beRight.which { out =>
          out must_== List(TextMessage("Hello!"))
        }
      }

      "iteratee3" in new WithApplication() {
        val controller = app.injector.instanceOf[Samples.Controller8]
        runWebSocket(controller.socket, Source.single(TextMessage("foo")), 1) must beRight.which { out =>
          out must_== List(TextMessage("I received your message: foo"))
        }
      }

    }
  }

  /**
   * The default await timeout.  Override this to change it.
   */
  import scala.concurrent.duration._
  override implicit def defaultAwaitTimeout = 2.seconds
}

object Samples {
  import Actor1.MyWebSocketActor
  //#actor-accept
  import play.api.mvc._
  import play.api.libs.streams._

  class Controller1 @Inject() (implicit system: ActorSystem, materializer: Materializer) {

    def socket = WebSocket.accept[String, String] { request =>
      ActorFlow.actorRef(out => MyWebSocketActor.props(out))
    }
  }
  //#actor-accept


  object Actor1 {

    //#example-actor
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
    //#example-actor
  }

  //#actor-try-accept
  import scala.concurrent.Future
  import play.api.mvc._
  import play.api.libs.streams._

  class Controller3 @Inject() (implicit system: ActorSystem, materializer: Materializer) extends play.api.mvc.Controller {
    def socket = WebSocket.acceptOrResult[String, String] { request =>
      Future.successful(request.session.get("user") match {
        case None => Left(Forbidden)
        case Some(_) => Right(ActorFlow.actorRef(MyWebSocketActor.props))
      })
    }
  }
  //#actor-try-accept

  //#actor-json
  import play.api.libs.json.JsValue
  import play.api.mvc._
  import play.api.libs.streams._

  class Controller4 @Inject() (implicit system: ActorSystem, materializer: Materializer) {
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

    def socket = WebSocket.accept[JsValue, JsValue] { request =>
      ActorFlow.actorRef(out => MyWebSocketActor.props(out))
    }

  }
  //#actor-json


  case class InEvent(foo: String)
  case class OutEvent(bar: String)

  //#actor-json-formats
  import play.api.libs.json._

  implicit val inEventFormat = Json.format[InEvent]
  implicit val outEventFormat = Json.format[OutEvent]
  //#actor-json-formats


  class Controller5 @Inject() (implicit system: ActorSystem, mat: Materializer) {
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

    //#actor-json-frames
    import play.api.mvc.WebSocket.FrameFormatter

    implicit val messageFlowTransformer = MessageFlowTransformer.jsonMessageFlowTransformer[InEvent, OutEvent]
    //#actor-json-frames

    //#actor-json-in-out
    import play.api.libs.json._
    import play.api.mvc._
    import play.api.libs.streams._

    // Note: requires implicit ActorSystem and Materializer (inject into your controller)
    def socket = WebSocket.accept[InEvent, OutEvent] { request =>
      ActorFlow.actorRef(out => MyWebSocketActor.props(out))
    }
    //#actor-json-in-out

  }

  class Controller6 {

    //#iteratee1
    import play.api.mvc._
    import play.api.libs.iteratee._
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def socket = WebSocket.using[String] { request =>

      // Log events to the console
      val in = Iteratee.foreach[String](println).map { _ =>
        println("Disconnected")
      }

      // Send a single 'Hello!' message
      val out = Enumerator("Hello!")

      (in, out)
    }
    //#iteratee1

  }

  class Controller7 {

    //#iteratee2
    import play.api.mvc._
    import play.api.libs.iteratee._

    def socket = WebSocket.using[String] { request =>

      // Just ignore the input
      val in = Iteratee.ignore[String]

      // Send a single 'Hello!' message and close
      val out = Enumerator("Hello!").andThen(Enumerator.eof)

      (in, out)
    }
    //#iteratee2

  }

  class Controller8 {

    //#iteratee3
    import play.api.mvc._
    import play.api.libs.iteratee._
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def socket = WebSocket.using[String] { request =>

      // Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
      val (out, channel) = Concurrent.broadcast[String]

      // log the message to stdout and send response back to client
      val in = Iteratee.foreach[String] {
        msg => println(msg)
          // the Enumerator returned by Concurrent.broadcast subscribes to the channel and will
          // receive the pushed messages
          channel push("I received your message: " + msg)
      }
      (in,out)
    }
    //#iteratee3
  }


}
