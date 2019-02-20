/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.dependencyinjection

import play.api.test._

class RuntimeDependencyInjection extends PlaySpecification {

  "Play's runtime dependency injection support" should {
    "support constructor injection" in new WithApplication() {
      app.injector.instanceOf[constructor.MyComponent] must beAnInstanceOf[constructor.MyComponent]
    }
    "support singleton scope" in new WithApplication() {
      app.injector.instanceOf[singleton.CurrentSharePrice].set(10)
      app.injector.instanceOf[singleton.CurrentSharePrice].get must_== 10
    }
    "support stopping" in {
      running() { app =>
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

  class MyComponent @Inject()(ws: WSClient) {
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
    def get         = price
  }
//#singleton
}

package cleanup {
  object MessageQueue {
    @volatile var stopped       = false
    def connectToMessageQueue() = MessageQueue
    def stop()                  = stopped = true
  }
  import MessageQueue.connectToMessageQueue

//#cleanup
  import scala.concurrent.Future
  import javax.inject._
  import play.api.inject.ApplicationLifecycle

  @Singleton
  class MessageQueueConnection @Inject()(lifecycle: ApplicationLifecycle) {
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

  class Module extends AbstractModule {
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

package dynamicguicemodule {

  import implemented._

//#dynamic-guice-module
  import com.google.inject.AbstractModule
  import com.google.inject.name.Names
  import play.api.Configuration
  import play.api.Environment

  class Module(environment: Environment, configuration: Configuration) extends AbstractModule {
    def configure() = {
      // Expect configuration like:
      // hello.en = "myapp.EnglishHello"
      // hello.de = "myapp.GermanHello"
      val helloConfiguration: Configuration =
        configuration.getOptional[Configuration]("hello").getOrElse(Configuration.empty)
      val languages: Set[String] = helloConfiguration.subKeys
      // Iterate through all the languages and bind the
      // class associated with that language. Use Play's
      // ClassLoader to load the classes.
      for (l <- languages) {
        val bindingClassName: String = helloConfiguration.get[String](l)
        val bindingClass: Class[_ <: Hello] =
          environment.classLoader
            .loadClass(bindingClassName)
            .asSubclass(classOf[Hello])
        bind(classOf[Hello])
          .annotatedWith(Names.named(l))
          .to(bindingClass)
      }
    }
  }
//#dynamic-guice-module
}

package eagerguicemodule {

  import implemented._

//#eager-guice-module
  import com.google.inject.AbstractModule
  import com.google.inject.name.Names

// A Module is needed to register bindings
  class Module extends AbstractModule {
    override def configure() = {

      // Bind the `Hello` interface to the `EnglishHello` implementation as eager singleton.
      bind(classOf[Hello])
        .annotatedWith(Names.named("en"))
        .to(classOf[EnglishHello])
        .asEagerSingleton()

      bind(classOf[Hello])
        .annotatedWith(Names.named("de"))
        .to(classOf[GermanHello])
        .asEagerSingleton()
    }
  }
//#eager-guice-module
}

package eagerguicestartup {

//#eager-guice-startup
  import scala.concurrent.Future
  import javax.inject._
  import play.api.inject.ApplicationLifecycle

// This creates an `ApplicationStart` object once at start-up and registers hook for shut-down.
  @Singleton
  class ApplicationStart @Inject()(lifecycle: ApplicationLifecycle) {
    // Shut-down hook
    lifecycle.addStopHook { () =>
      Future.successful(())
    }
    //...
  }
//#eager-guice-startup
}

package eagerguicemodulestartup {

  import eagerguicestartup._
//#eager-guice-module-startup
  import com.google.inject.AbstractModule

  class StartModule extends AbstractModule {
    override def configure() = {
      bind(classOf[ApplicationStart]).asEagerSingleton()
    }
  }
//#eager-guice-module-startup
}

package playmodule {

  import play.api.Configuration
  import play.api.Environment

  import implemented._

//#play-module
  import play.api.inject._

  class HelloModule extends Module {
    def bindings(environment: Environment, configuration: Configuration) = Seq(
      bind[Hello].qualifiedWith("en").to[EnglishHello],
      bind[Hello].qualifiedWith("de").to[GermanHello]
    )
  }
//#play-module
}

package eagerplaymodule {

  import play.api.Configuration
  import play.api.Environment

  import implemented._

//#eager-play-module
  import play.api.inject._

  class HelloModule extends Module {
    def bindings(environment: Environment, configuration: Configuration) = Seq(
      bind[Hello].qualifiedWith("en").to[EnglishHello].eagerly(),
      bind[Hello].qualifiedWith("de").to[GermanHello].eagerly()
    )
  }
//#eager-play-module
}
package injected.controllers {
  import play.api.mvc._
  class Application {
    def index = Action(Results.Ok)
  }
}

package customapplicationloader {

//#custom-application-loader
  import play.api.ApplicationLoader
  import play.api.Configuration
  import play.api.inject._
  import play.api.inject.guice._

  class CustomApplicationLoader extends GuiceApplicationLoader() {
    override def builder(context: ApplicationLoader.Context): GuiceApplicationBuilder = {
      val extra = Configuration("a" -> 1)
      initialBuilder
        .in(context.environment)
        .loadConfig(extra ++ context.initialConfiguration)
        .overrides(overrides(context): _*)
    }
  }
//#custom-application-loader
}

package circular {

//#circular
  import javax.inject.Inject

  class Foo @Inject()(bar: Bar)
  class Bar @Inject()(baz: Baz)
  class Baz @Inject()(foo: Foo)
//#circular

}

package circularProvider {

//#circular-provider
  import javax.inject.Inject
  import javax.inject.Provider

  class Foo @Inject()(bar: Bar)
  class Bar @Inject()(baz: Baz)
  class Baz @Inject()(foo: Provider[Foo])
//#circular-provider

}
