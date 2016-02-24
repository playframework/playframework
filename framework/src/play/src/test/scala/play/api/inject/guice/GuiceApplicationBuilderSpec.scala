/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject
package guice

import javax.inject.{ Inject, Provider, Singleton }

import com.google.inject.{ ProvisionException, CreationException }
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment }

object GuiceApplicationBuilderSpec extends Specification {

  "GuiceApplicationBuilder" should {

    "add bindings" in {
      val injector = new GuiceApplicationBuilder()
        .bindings(
          new AModule,
          bind[B].to[B1])
        .injector

      injector.instanceOf[A] must beAnInstanceOf[A1]
      injector.instanceOf[B] must beAnInstanceOf[B1]
    }

    "override bindings" in {
      val app = new GuiceApplicationBuilder()
        .bindings(new AModule)
        .overrides(
          bind[Configuration] to new ExtendConfiguration("a" -> 1),
          bind[A].to[A2])
        .build

      app.configuration.getInt("a") must beSome(1)
      app.injector.instanceOf[A] must beAnInstanceOf[A2]
    }

    "disable modules" in {
      val injector = new GuiceApplicationBuilder()
        .bindings(new AModule)
        .disable[play.api.i18n.I18nModule]
        .disable(classOf[AModule])
        .injector

      injector.instanceOf[play.api.i18n.Langs] must throwA[com.google.inject.ConfigurationException]
      injector.instanceOf[A] must throwA[com.google.inject.ConfigurationException]
    }

    "set initial configuration loader" in {
      val extraConfig = Configuration("a" -> 1)
      val app = new GuiceApplicationBuilder()
        .loadConfig(env => Configuration.load(env) ++ extraConfig)
        .build

      app.configuration.getInt("a") must beSome(1)
    }

    "set module loader" in {
      val injector = new GuiceApplicationBuilder()
        .load((env, conf) => Seq(new BuiltinModule, bind[A].to[A1]))
        .injector

      injector.instanceOf[A] must beAnInstanceOf[A1]
    }

    "set loaded modules directly" in {
      val injector = new GuiceApplicationBuilder()
        .load(new BuiltinModule, bind[A].to[A1])
        .injector

      injector.instanceOf[A] must beAnInstanceOf[A1]
    }

    "eagerly load singletons" in {
      new GuiceApplicationBuilder()
        .load(new BuiltinModule, bind[C].to[C1])
        .eagerlyLoaded()
        .injector() must throwAn[CreationException]
    }

    "set lazy load singletons" in {
      val builder = new GuiceApplicationBuilder()
        .load(new BuiltinModule, bind[C].to[C1])

      builder.injector() must throwAn[CreationException].not
      builder.injector().instanceOf[C] must throwAn[ProvisionException]
    }

    "display logger deprecation message" in {
      List("logger", "logger.resource", "logger.resource.test").forall { path =>
        List("DEBUG", "WARN", "INFO", "ERROR", "TRACE", "OFF").forall { value =>
          val data = Map(path -> value)
          val builder = new GuiceApplicationBuilder()
          builder.shouldDisplayLoggerDeprecationMessage(Configuration.from(data)) must_=== true
        }
      }
    }

    "not display logger deprecation message" in {
      List("logger", "logger.resource", "logger.resource.test").forall { path =>
        val data = Map(path -> "NOT_A_DEPRECATED_VALUE")
        val builder = new GuiceApplicationBuilder()
        builder.shouldDisplayLoggerDeprecationMessage(Configuration.from(data)) must_=== false
      }
    }
  }

  trait A
  class A1 extends A
  class A2 extends A

  class AModule extends Module {
    def bindings(env: Environment, conf: Configuration) = Seq(
      bind[A].to[A1]
    )
  }

  trait B
  class B1 extends B

  class ExtendConfiguration(conf: (String, Any)*) extends Provider[Configuration] {
    @Inject
    var injector: Injector = _
    lazy val get = {
      val current = injector.instanceOf[ConfigurationProvider].get
      current ++ Configuration.from(conf.toMap)
    }
  }

  trait C

  @Singleton
  class C1 extends C {
    throw new EagerlyLoadedException
  }

  class EagerlyLoadedException extends RuntimeException

}
