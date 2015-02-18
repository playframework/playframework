/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.advanced.dependencyinjection

import play.api.test._

object RuntimeDependencyInjection extends PlaySpecification {

  "Play's runtime dependency injection support" should {
    "support constructor injection" in new WithApplication() {
      app.injector.instanceOf[constructor.MyComponent] must beAnInstanceOf[constructor.MyComponent]
    }
    "support singleton scope" in new WithApplication() {
      app.injector.instanceOf[singleton.CurrentSharePrice].set(10)
      app.injector.instanceOf[singleton.CurrentSharePrice].get must_== 10
    }
    "support stopping" in {
      val app = FakeApplication()
      running(app) {
        app.injector.instanceOf[cleanup.MessageQueueConnection]
      }
      cleanup.MessageQueue.stopped must_== true
    }
    "support implemented by annotation" in new WithApplication() {
      app.injector.instanceOf[implemented.Hello].sayHello("world") must_== "Hello world"
    }
  }

}

package constructor {
//#constructor
import javax.inject._
import play.api.libs.ws._

class MyComponent @Inject() (ws: WSClient) {
  // ...
}
//#constructor
}

package singleton {
//#singleton
import javax.inject._

@Singleton
class CurrentSharePrice {
  @volatile private var price = 0

  def set(p: Int) = price = p
  def get = price
}
//#singleton
}

package cleanup {
object MessageQueue {
  @volatile var stopped = false
  def connectToMessageQueue() = MessageQueue
  def stop() = stopped = true
}
import MessageQueue.connectToMessageQueue

//#cleanup
import scala.concurrent.Future
import javax.inject._
import play.api.inject.ApplicationLifecycle

@Singleton
class MessageQueueConnection @Inject() (lifecycle: ApplicationLifecycle) {
  val connection = connectToMessageQueue()
  lifecycle.addStopHook { () =>
    Future.successful(connection.stop())
  }

  //...
}
//#cleanup
}

package implemented {
//#implemented-by
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[EnglishHello])
trait Hello {
  def sayHello(name: String): String
}

class EnglishHello extends Hello {
  def sayHello(name: String) = "Hello " + name
}
//#implemented-by
class GermanHello extends Hello {
  def sayHello(name: String) = "Hallo " + name
}
}

package guicemodule {

import implemented._

//#guice-module
import com.google.inject.AbstractModule
import com.google.inject.name.Names
  
class HelloModule extends AbstractModule {
  def configure() = {

    bind(classOf[Hello])
      .annotatedWith(Names.named("en"))
      .to(classOf[EnglishHello])

    bind(classOf[Hello])
      .annotatedWith(Names.named("de"))
      .to(classOf[GermanHello])
  }
}
//#guice-module
}

package playmodule {

import play.api.{Configuration, Environment}

import implemented._

//#play-module
import play.api.inject._

class HelloModule extends Module {
  def bindings(environment: Environment,
               configuration: Configuration) = Seq(
    bind[Hello].qualifiedWith("en").to[EnglishHello],
    bind[Hello].qualifiedWith("de").to[GermanHello]
  )
}
//#play-module
}
