/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.extending

import play.api.test._

object ScalaPlugins extends PlaySpecification {
  "scala plugins" should {
    "allow accessing plugins" in {
      val app = FakeApplication(additionalPlugins = Seq(classOf[MyPlugin].getName))
      var mc: MyComponent = null
      running(app) {
        //#access-plugin
        import play.api.Play
        import play.api.Play.current

        val myComponent = Play.application.plugin[MyPlugin]
          .getOrElse(throw new RuntimeException("MyPlugin not loaded"))
          .myComponent
        //#access-plugin
        myComponent.started must beTrue
        mc = myComponent
      }
      mc.stopped must beTrue
    }
    "allow the actors example to work" in {
      val app = FakeApplication(additionalPlugins = Seq(classOf[Actors].getName))
      running(app) {
        import scala.concurrent.duration._
        import akka.pattern.ask
        implicit def timeout = 20.seconds
        await(Actors.myActor ? "hi") must_== "hi"
      }
    }
  }
}

//#my-plugin
//###insert: package plugins

import play.api.{Plugin, Application}

class MyPlugin extends Plugin {
  val myComponent = new MyComponent()

  override def onStart() = {
    myComponent.start()
  }

  override def onStop() = {
    myComponent.stop()
  }

  override def enabled = true
}
//#my-plugin

class MyComponent() {
  var started: Boolean = false
  var stopped: Boolean = false
  def start() = started = true
  def stop() = stopped = true
}

//#actor-example
//###insert: package actors

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import javax.inject.Inject

class Actors @Inject() (implicit app: Application) extends Plugin {
  lazy val myActor = Akka.system.actorOf(MyActor.props, "my-actor")
}

object Actors {
  def myActor: ActorRef = Play.current.plugin[Actors]
    .getOrElse(throw new RuntimeException("Actors plugin not loaded"))
    .myActor
}
//#actor-example

object MyActor {
  def props = Props(classOf[MyActor])
}

class MyActor extends Actor {
  def receive = {
    case msg => sender ! msg
  }
}
