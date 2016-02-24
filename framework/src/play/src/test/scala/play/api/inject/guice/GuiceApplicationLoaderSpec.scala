/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject.guice

import org.specs2.mutable.Specification

import com.google.inject.AbstractModule

import play.{ Configuration => JavaConfiguration, Environment => JavaEnvironment }
import play.api.{ ApplicationLoader, Configuration, Environment }
import play.api.inject.BuiltinModule

class GuiceApplicationLoaderSpec extends Specification {

  "GuiceApplicationLoader" should {

    "allow adding additional modules" in {
      val module = new AbstractModule {
        def configure() = {
          bind(classOf[Bar]) to classOf[MarsBar]
        }
      }
      val builder = new GuiceApplicationBuilder().bindings(module)
      val loader = new GuiceApplicationLoader(builder)
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Bar] must beAnInstanceOf[MarsBar]
    }

    "allow replacing automatically loaded modules" in {
      val builder = new GuiceApplicationBuilder().load(new BuiltinModule, new ManualTestModule)
      val loader = new GuiceApplicationLoader(builder)
      val app = loader.load(fakeContext)
      app.injector.instanceOf[Foo] must beAnInstanceOf[ManualFoo]
    }

    "load static Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app = loader.load(fakeContextWithModule(classOf[StaticTestModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[StaticFoo]
    }

    "load dynamic Scala Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app = loader.load(fakeContextWithModule(classOf[ScalaConfiguredModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[ScalaConfiguredFoo]
    }

    "load dynamic Java Guice modules from configuration" in {
      val loader = new GuiceApplicationLoader()
      val app = loader.load(fakeContextWithModule(classOf[JavaConfiguredModule]))
      app.injector.instanceOf[Foo] must beAnInstanceOf[JavaConfiguredFoo]
    }

  }

  def fakeContext = ApplicationLoader.createContext(Environment.simple())
  def fakeContextWithModule(module: Class[_ <: AbstractModule]) = {
    val f = fakeContext
    val c = f.initialConfiguration
    val newModules: Seq[String] = c.getStringSeq("play.modules.enabled").fold(Seq.empty[String]) { oldModules =>
      oldModules :+ module.getName
    }
    val modulesConf = Configuration("play.modules.enabled" -> newModules)
    val combinedConf = f.initialConfiguration ++ modulesConf
    f.copy(initialConfiguration = combinedConf)
  }
}

class ManualTestModule extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[Foo]) to classOf[ManualFoo]
  }
}

class StaticTestModule extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[Foo]) to classOf[StaticFoo]
  }
}

class ScalaConfiguredModule(
    environment: Environment,
    configuration: Configuration) extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[Foo]) to classOf[ScalaConfiguredFoo]
  }
}
class JavaConfiguredModule(
    environment: JavaEnvironment,
    configuration: JavaConfiguration) extends AbstractModule {
  def configure(): Unit = {
    bind(classOf[Foo]) to classOf[JavaConfiguredFoo]
  }
}

trait Bar
class MarsBar extends Bar

trait Foo
class ManualFoo extends Foo
class StaticFoo extends Foo
class ScalaConfiguredFoo extends Foo
class JavaConfiguredFoo extends Foo
