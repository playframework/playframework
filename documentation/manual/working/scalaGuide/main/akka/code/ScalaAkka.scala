/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.akka {

import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test._
import java.io.File

@RunWith(classOf[JUnitRunner])
class ScalaAkkaSpec extends PlaySpecification {

  def withActorSystem[T](block: ActorSystem => T) = {
    val system = ActorSystem()
    try {
      block(system)
    } finally {
      system.shutdown()
      system.awaitTermination()
    }
  }
  
  "The Akka support" should {

    "allow injecting actors" in new WithApplication() {
      import controllers._
      val controller = app.injector.instanceOf[Application]
      
      val helloActor = controller.helloActor
      import play.api.mvc._
      import play.api.mvc.Results._
      import actors.HelloActor.SayHello

      //#ask
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      import scala.concurrent.duration._
      import akka.pattern.ask
      implicit val timeout = 5.seconds
      
      def sayHello(name: String) = Action.async {
        (helloActor ? SayHello(name)).mapTo[String].map { message =>
          Ok(message)
        }
      }
      //#ask
      
      contentAsString(sayHello("world")(FakeRequest())) must_== "Hello, world"
    }

    "allow using the scheduler" in withActorSystem { system =>
      import akka.actor._
      val testActor = system.actorOf(Props(new Actor() {
        def receive = { case _: String => }
      }), name = "testActor")
      //#schedule-actor
      import scala.concurrent.duration._

      val cancellable = system.scheduler.schedule(
        0.microseconds, 300.microseconds, testActor, "tick")
      //#schedule-actor
      ok
    }

    "actor scheduler" in withActorSystem { system =>
      val file = new File("/tmp/nofile")
      file.mkdirs()
      //#schedule-callback
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      system.scheduler.scheduleOnce(10.milliseconds) {
        file.delete()
      }
      //#schedule-callback
      Thread.sleep(200)
      file.exists() must beFalse
    }
  }
}

package controllers {
//#controller
import play.api.mvc._
import akka.actor._
import javax.inject._
  
import actors.HelloActor

@Singleton
class Application @Inject() (system: ActorSystem) extends Controller {

  val helloActor = system.actorOf(HelloActor.props, "hello-actor")
  
  //...
}
//#controller  
}

package actors {
//#actor
import akka.actor._
  
object HelloActor {
  def props = Props[HelloActor]
  
  case class SayHello(name: String)
}

class HelloActor extends Actor {
  import HelloActor._
  
  def receive = {
    case SayHello(name: String) =>
      sender() ! "Hello, " + name
  }
}
//#actor
}

}
