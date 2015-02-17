/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.async.websockets

import play.api.test._
import scala.concurrent.Promise

object ScalaWebSockets extends PlaySpecification {

  import java.io.Closeable
  import play.api.libs.iteratee._
  import play.api.mvc.{Result, WebSocket}
  import play.api.libs.json.{Json, JsValue}

  "Scala WebSockets" should {

    "support actors" in {

      def runWebSocket[In, Out](webSocket: WebSocket[In, Out], in: Enumerator[In]): Either[Result, List[Out]] = {
        await(webSocket.f(FakeRequest())).right.map { f =>
          val consumed = Promise[List[Out]]()
          def getChunks(chunks: List[Out]): Iteratee[Out, Unit] = Cont {
            case Input.El(out) => getChunks(out :: chunks)
            case Input.EOF => Done(consumed.success(chunks.reverse))
            case Input.Empty => getChunks(chunks)
          }
          f(in, getChunks(Nil))
          await(consumed.future)
        }

      }

      import akka.actor._

      "allow creating a simple echoing actor" in new WithApplication() {
        runWebSocket(Samples.Controller1.socket, Enumerator("foo") >>> Enumerator.eof) must beRight.like {
          case list => list must_== List("I received your message: foo")
        }
      }

      "allow cleaning up" in new WithApplication() {
        @volatile var closed = false
        val someResource = new Closeable() {
          def close() = closed = true
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
          WebSocket.acceptWithActor[String, String](req => out => Props(new MyActor)), Enumerator.eof
        ) must beRight[List[String]]
        closed must beTrue
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
          WebSocket.acceptWithActor[String, String](req => out => Props(new MyActor)), Enumerator.empty
        ) must beRight[List[String]]
      }

      "allow rejecting the WebSocket" in new WithApplication() {
        runWebSocket(Samples.Controller2.socket, Enumerator.empty) must beLeft.which { result =>
          result.header.status must_== FORBIDDEN
        }
      }

      "allow creating a json actor" in new WithApplication() {
        val json = Json.obj("foo" -> "bar")
        runWebSocket(Samples.Controller4.socket, Enumerator[JsValue](json) >>> Enumerator.eof) must beRight.which { out =>
          out must_== List(json)
        }
      }

      "allow creating a higher level object actor" in new WithApplication() {
        runWebSocket(
          Samples.Controller5.socket,
          Enumerator(Samples.Controller5.InEvent("blah")) >>> Enumerator.eof
        ) must beRight.which { out =>
          out must_== List(Samples.Controller5.OutEvent("blah"))
        }
      }

    }

    "support iteratees" in {

      def runWebSocket[In, Out](webSocket: WebSocket[In, Out], in: Enumerator[In]): Either[Result, List[Out]] = {
        await(webSocket.f(FakeRequest())).right.map { f =>
          val consumed = Promise[List[Out]]()
          @volatile var chunks = List.empty[Out]
          def getChunks: Iteratee[Out, Unit] = Cont {
            case Input.El(out) =>
              chunks = out :: chunks
              getChunks
            case Input.EOF => Done(consumed.trySuccess(chunks.reverse))
            case Input.Empty => getChunks
          }

          import scala.concurrent.ExecutionContext.Implicits.global
          import scala.concurrent.duration._
          f(in.onDoneEnumerating {
            // Yeah, ugly, but it makes a race condition unlikely.
            play.api.libs.concurrent.Promise.timeout((), 100.milliseconds).onSuccess {
              case _ => consumed.trySuccess(chunks)
            }
          }, getChunks)
          await(consumed.future)
        }
      }

      "iteratee1" in new WithApplication() {
        runWebSocket(Samples.Controller6.socket, Enumerator.eof) must beRight.which { out =>
          out must_== List("Hello!")
        }
      }

      "iteratee2" in new WithApplication() {
        runWebSocket(Samples.Controller7.socket, Enumerator.empty) must beRight.which { out =>
          out must_== List("Hello!")
        }
      }

      "iteratee3" in new WithApplication() {
        runWebSocket(Samples.Controller8.socket, Enumerator("foo") >>> Enumerator.eof) must beRight.which { out =>
          out must_== List("I received your message: foo")
        }
      }

    }
  }

}

object Samples {
  object Controller1 {
    import Actor1.MyWebSocketActor

    //#actor-accept
    import play.api.mvc._
    import play.api.Play.current

    def socket = WebSocket.acceptWithActor[String, String] { request => out =>
      MyWebSocketActor.props(out)
    }
    //#actor-accept
  }

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

  object Controller2 extends play.api.mvc.Controller {
    import Actor1.MyWebSocketActor

    //#actor-try-accept
    import scala.concurrent.Future
    import play.api.mvc._
    import play.api.Play.current

    def socket = WebSocket.tryAcceptWithActor[String, String] { request =>
      Future.successful(request.session.get("user") match {
        case None => Left(Forbidden)
        case Some(_) => Right(MyWebSocketActor.props)
      })
    }
    //#actor-try-accept
  }

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

    //#actor-json
    import play.api.mvc._
    import play.api.libs.json._
    import play.api.Play.current

    def socket = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
      MyWebSocketActor.props(out)
    }
    //#actor-json

  }

  object Controller5 {
    import akka.actor._

    case class InEvent(foo: String)
    case class OutEvent(bar: String)

    class MyWebSocketActor(out: ActorRef) extends Actor {
      def receive = {
        case InEvent(foo) =>
          out ! OutEvent(foo)
      }
    }

    object MyWebSocketActor {
      def props(out: ActorRef) = Props(new MyWebSocketActor(out))
    }

    //#actor-json-formats
    import play.api.libs.json._

    implicit val inEventFormat = Json.format[InEvent]
    implicit val outEventFormat = Json.format[OutEvent]
    //#actor-json-formats

    //#actor-json-frames
    import play.api.mvc.WebSocket.FrameFormatter

    implicit val inEventFrameFormatter = FrameFormatter.jsonFrame[InEvent]
    implicit val outEventFrameFormatter = FrameFormatter.jsonFrame[OutEvent]
    //#actor-json-frames

    //#actor-json-in-out
    import play.api.mvc._
    import play.api.Play.current

    def socket = WebSocket.acceptWithActor[InEvent, OutEvent] { request => out =>
      MyWebSocketActor.props(out)
    }
    //#actor-json-in-out

  }

  object Controller6 {

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

  object Controller7 {

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

  object Controller8 {

    //#iteratee3
    import play.api.mvc._
    import play.api.libs.iteratee._
    import play.api.libs.concurrent.Execution.Implicits.defaultContext

    def socket =  WebSocket.using[String] { request =>

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
