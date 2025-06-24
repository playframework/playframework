/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject.guice

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.concurrent.Future

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.ClassicActorSystemProvider
import org.specs2.mutable.Specification
import play.{ Environment => JavaEnvironment }
import play.api.i18n.I18nModule
import play.api.inject.BuiltinModule
import play.api.inject.DefaultApplicationLifecycle
import play.api.mvc.CookiesModule
import play.api.ApplicationLoader
import play.api.Configuration
import play.api.Environment

class GuiceApplicationLoaderSpec extends Specification {
  "GuiceApplicationLoader" should {
    "allow adding additional modules" in {
      val module = new AbstractModule {
        override def configure() = {
          bind(classOf[Bar]) to classOf[MarsBar]
        }
      }
      val builder = new GuiceApplicationBuilder().bindings(module)
      val loader  = new GuiceApplicationLoader(builder)
      val app     = loader.load(fakeContext)
      app.injector.instanceOf[Bar] must beAnInstanceOf[MarsBar]
    }

    "allow replacing automatically loaded modules" in {
      val builder =
        new GuiceApplicationBuilder().load(new BuiltinModule, new I18nModule, new CookiesModule, new ManualTestModule)
      val loader = new GuiceApplicationLoader(builder)
      val app    = loader.load(fakeContext)
      app.injector.instanceOf[Foo] must beAnInstanceOf[ManualFoo]
    }

    "load static Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app    = loader.load(fakeContextWithModule(classOf[StaticTestModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[StaticFoo]
    }

    "load dynamic Scala Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app    = loader.load(fakeContextWithModule(classOf[ScalaConfiguredModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[ScalaConfiguredFoo]
    }

    "load dynamic Java Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app    = loader.load(fakeContextWithModule(classOf[JavaConfiguredModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[JavaConfiguredFoo]
    }

    "provide an ClassicActorSystem " in {
      val loader                                            = new GuiceApplicationLoader()
      val application                                       = loader.load(fakeContext)
      val system                                            = application.injector.instanceOf[ActorSystem]
      val classicSystemProvider: ClassicActorSystemProvider =
        application.injector.instanceOf[ClassicActorSystemProvider]
      system must_!= null
      system must_== classicSystemProvider.asInstanceOf[ActorSystem]
    }

    "call the stop hooks from the context" in {
      val lifecycle   = new DefaultApplicationLifecycle
      var hooksCalled = false
      lifecycle.addStopHook(() => Future.successful { hooksCalled = true })
      val loader = new GuiceApplicationLoader()
      val app    = loader.load(ApplicationLoader.Context.create(Environment.simple(), lifecycle = lifecycle))
      Await.ready(app.stop(), 5.minutes)
      hooksCalled must_== true
    }
  }

  def fakeContext: ApplicationLoader.Context                                               = ApplicationLoader.Context.create(Environment.simple())
  def fakeContextWithModule(module: Class[_ <: AbstractModule]): ApplicationLoader.Context = {
    val f                       = fakeContext
    val c                       = f.initialConfiguration
    val newModules: Seq[String] = c.get[Seq[String]]("play.modules.enabled") :+ module.getName
    val modulesConf             = Configuration("play.modules.enabled" -> newModules)
    val combinedConf            = modulesConf.withFallback(f.initialConfiguration)
    f.copy(initialConfiguration = combinedConf)
  }
}

class ManualTestModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Foo]) to classOf[ManualFoo]
  }
}

class StaticTestModule extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Foo]) to classOf[StaticFoo]
  }
}

class ScalaConfiguredModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Foo]) to classOf[ScalaConfiguredFoo]
  }
}
class JavaConfiguredModule(environment: JavaEnvironment, config: Config) extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[Foo]) to classOf[JavaConfiguredFoo]
  }
}

trait Bar
class MarsBar extends Bar

trait Foo
class ManualFoo          extends Foo
class StaticFoo          extends Foo
class ScalaConfiguredFoo extends Foo
class JavaConfiguredFoo  extends Foo
