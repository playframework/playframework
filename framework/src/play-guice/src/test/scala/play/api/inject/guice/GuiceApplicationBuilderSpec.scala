/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject
package guice

import java.util.Collections

import com.google.inject.CreationException
import com.google.inject.Guice
import com.google.inject.ProvisionException
import com.typesafe.config.Config
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.i18n.I18nModule
import play.api.mvc.CookiesModule
import play.core.WebCommands
import play.inject.{ Module => JavaModule }
import play.{ Environment => JavaEnvironment }

class GuiceApplicationBuilderSpec extends Specification {

  "GuiceApplicationBuilder" should {

    "add bindings with Scala" in {
      addBindings(new GuiceApplicationBuilderSpec.AModule)
    }

    "add bindings with Java" in {
      addBindings(new GuiceApplicationBuilderSpec.JavaAModule)
    }

    def addBindings(module: Module) = {
      val injector = new GuiceApplicationBuilder()
        .bindings(
          module,
          bind[GuiceApplicationBuilderSpec.B].to[GuiceApplicationBuilderSpec.B1])
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
      injector.instanceOf[GuiceApplicationBuilderSpec.B] must beAnInstanceOf[GuiceApplicationBuilderSpec.B1]
    }

    "override bindings with Scala" in {
      overrideBindings(new GuiceApplicationBuilderSpec.AModule)
    }

    "override bindings with Java" in {
      overrideBindings(new GuiceApplicationBuilderSpec.JavaAModule)
    }

    def overrideBindings(module: Module) = {
      val app = new GuiceApplicationBuilder()
        .bindings(module)
        .overrides(
          bind[Configuration] to new GuiceApplicationBuilderSpec.ExtendConfiguration("a" -> 1),
          bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A2])
        .build()

      app.configuration.get[Int]("a") must_== 1
      app.injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A2]
    }

    "disable modules with Scala" in {
      disableModules(new GuiceApplicationBuilderSpec.AModule)
    }

    "disable modules with Java" in {
      disableModules(new GuiceApplicationBuilderSpec.JavaAModule)
    }

    def disableModules(module: Module) = {
      val injector = new GuiceApplicationBuilder()
        .bindings(module)
        .disable(module.getClass)
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must throwA[com.google.inject.ConfigurationException]
    }

    "set initial configuration loader" in {
      val extraConfig = Configuration("a" -> 1)
      val app = new GuiceApplicationBuilder()
        .loadConfig(env => Configuration.load(env) ++ extraConfig)
        .build()

      app.configuration.get[Int]("a") must_== 1
    }

    "set module loader" in {
      val injector = new GuiceApplicationBuilder()
        .load((env, conf) => Seq(new BuiltinModule, new I18nModule, new CookiesModule, bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A1]))
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
    }

    "set loaded modules directly" in {
      val injector = new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, new CookiesModule, bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A1])
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
    }

    "eagerly load singletons" in {
      new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, new CookiesModule, bind[GuiceApplicationBuilderSpec.C].to[GuiceApplicationBuilderSpec.C1])
        .eagerlyLoaded()
        .injector() must throwA[CreationException]
    }

    "work with built in modules and requireAtInjectOnConstructors" in {
      new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, new CookiesModule)
        .requireAtInjectOnConstructors()
        .eagerlyLoaded()
        .injector() must not(throwA[CreationException])
    }

    "set lazy load singletons" in {
      val builder = new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, new CookiesModule, bind[GuiceApplicationBuilderSpec.C].to[GuiceApplicationBuilderSpec.C1])

      builder.injector() must throwAn[CreationException].not
      builder.injector().instanceOf[GuiceApplicationBuilderSpec.C] must throwAn[ProvisionException]
    }

    "bind a unique singleton instance of WebCommands" in {
      val applicationModule = new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, new CookiesModule)
        .applicationModule()
      val injector1 = Guice.createInjector(applicationModule)
      val injector2 = Guice.createInjector(applicationModule)
      injector1.getInstance(classOf[WebCommands]) must_=== injector1.getInstance(classOf[WebCommands])
      injector2.getInstance(classOf[WebCommands]) must_!== injector1.getInstance(classOf[WebCommands])
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

}

object GuiceApplicationBuilderSpec {

  class ExtendConfiguration(conf: (String, Any)*) extends Provider[Configuration] {
    @Inject
    var injector: Injector = _
    lazy val get = {
      val current = injector.instanceOf[ConfigurationProvider].get
      current ++ Configuration.from(conf.toMap)
    }
  }

  trait A
  class A1 extends A
  class A2 extends A

  class AModule extends SimpleModule(bind[A].to[A1])

  trait B
  class B1 extends B

  trait C

  @Singleton
  class C1 extends C {
    throw new EagerlyLoadedException
  }

  class JavaAModule extends JavaModule {
    override def bindings(environment: JavaEnvironment, config: Config) = Collections.singletonList(JavaModule.bindClass(classOf[A]).to(classOf[A1]))
  }

  class EagerlyLoadedException extends RuntimeException

}
