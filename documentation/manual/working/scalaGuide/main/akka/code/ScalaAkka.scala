/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.akka {

import akka.actor.ActorSystem
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test._
import java.io.File

class ScalaAkkaSpec extends PlaySpecification {

  sequential

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

    "allow binding actors" in new WithApplication(new GuiceApplicationBuilder()
      .bindings(new modules.MyModule)
      .configure("my.config" -> "foo")
      .build()
    ) {
      import injection._
      val controller = app.injector.instanceOf[Application]
      contentAsString(controller.getConfig(FakeRequest())) must_== "foo"
    }

    "allow binding actor factories" in new WithApplication(new GuiceApplicationBuilder()
      .bindings(new factorymodules.MyModule)
      .configure("my.config" -> "foo")
      .build()
    ) {
      import play.api.inject.bind
      import akka.actor._
      import play.api.libs.concurrent.Execution.Implicits.defaultContext
      import scala.concurrent.duration._
      import akka.pattern.ask
      implicit val timeout = 5.seconds

      val actor = app.injector.instanceOf(bind[ActorRef].qualifiedWith("parent-actor"))
      val futureConfig = for {
        child <- (actor ? actors.ParentActor.GetChild("my.config")).mapTo[ActorRef]
        config <- (child ? actors.ConfiguredChildActor.GetConfig).mapTo[String]
      } yield config
      await(futureConfig) must_== "foo"
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

package injection {
//#inject
import play.api.mvc._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import javax.inject._
import actors.ConfiguredActor._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

@Singleton
class Application @Inject() (@Named("configured-actor") configuredActor: ActorRef)
                            (implicit ec: ExecutionContext) extends Controller {

  implicit val timeout: Timeout = 5.seconds

  def getConfig = Action.async {
    (configuredActor ? GetConfig).mapTo[String].map { message =>
      Ok(message)
    }
  }
}
//#inject
}

package modules {
//#binding
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors.ConfiguredActor

class MyModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[ConfiguredActor]("configured-actor")
  }
}
//#binding
}

package factorymodules {
//#factorybinding
import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

import actors._

class MyModule extends AbstractModule with AkkaGuiceSupport {
  def configure = {
    bindActor[ParentActor]("parent-actor")
    bindActorFactory[ConfiguredChildActor, ConfiguredChildActor.Factory]
  }
}
//#factorybinding
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

//#injected
import akka.actor._
import javax.inject._
import play.api.Configuration

object ConfiguredActor {
  case object GetConfig
}

class ConfiguredActor @Inject() (configuration: Configuration) extends Actor {
  import ConfiguredActor._

  val config = configuration.getString("my.config").getOrElse("none")

  def receive = {
    case GetConfig =>
      sender() ! config
  }
}
//#injected

//#injectedchild
import akka.actor._
import javax.inject._
import com.google.inject.assistedinject.Assisted
import play.api.Configuration

object ConfiguredChildActor {
  case object GetConfig

  trait Factory {
    def apply(key: String): Actor
  }
}

class ConfiguredChildActor @Inject() (configuration: Configuration,
    @Assisted key: String) extends Actor {
  import ConfiguredChildActor._

  val config = configuration.getString(key).getOrElse("none")

  def receive = {
    case GetConfig =>
      sender() ! config
  }
}
//#injectedchild

//#injectedparent
import akka.actor._
import javax.inject._
import play.api.libs.concurrent.InjectedActorSupport

object ParentActor {
  case class GetChild(key: String)
}

class ParentActor @Inject() (
    childFactory: ConfiguredChildActor.Factory
) extends Actor with InjectedActorSupport {
  import ParentActor._

  def receive = {
    case GetChild(key: String) =>
      val child: ActorRef = injectedChild(childFactory(key), key)
      sender() ! child
  }
}
//#injectedparent

}

}
