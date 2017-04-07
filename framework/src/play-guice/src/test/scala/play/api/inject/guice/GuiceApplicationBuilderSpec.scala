/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.inject
package guice

import javax.inject.{ Inject, Provider, Singleton }

import com.google.inject.{ CreationException, ProvisionException }
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.i18n.I18nModule
import play.api.inject.guice.GuiceApplicationBuilderSpec.RequestScopedRoutes
import play.api.mvc.Results._
import play.api.mvc.{ DefaultActionBuilder, EssentialAction, RequestHeader }
import play.api.routing.Router
import play.core.test.FakeRequest

import scala.concurrent.duration._

class GuiceApplicationBuilderSpec extends Specification {

  "GuiceApplicationBuilder" should {

    "add bindings" in {
      val injector = new GuiceApplicationBuilder()
        .bindings(
          new GuiceApplicationBuilderSpec.AModule,
          bind[GuiceApplicationBuilderSpec.B].to[GuiceApplicationBuilderSpec.B1])
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
      injector.instanceOf[GuiceApplicationBuilderSpec.B] must beAnInstanceOf[GuiceApplicationBuilderSpec.B1]
    }

    "override bindings" in {
      val app = new GuiceApplicationBuilder()
        .bindings(new GuiceApplicationBuilderSpec.AModule)
        .overrides(
          bind[Configuration] to new GuiceApplicationBuilderSpec.ExtendConfiguration("a" -> 1),
          bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A2])
        .build()

      app.configuration.get[Int]("a") must_== 1
      app.injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A2]
    }

    "allow defining a custom request-scoped router" in {
      val app = new GuiceApplicationBuilder()
        .overrides(
          bind[Router].to[RequestScopedRoutes]
        )
        .build()

      import scala.concurrent.Await

      val router = app.injector.instanceOf[Router]
      import app.materializer
      val request = FakeRequest("GET", "/echo?message=foo")
      val action = router.handlerFor(request).get.asInstanceOf[EssentialAction]
      val resultFuture = action(request).run()
      val result = Await.result(resultFuture, 1.second)

      val bodyString = Await.result(result.body.consumeData, 1.second).utf8String

      bodyString must_== "foo"
    }

    "disable modules" in {
      val injector = new GuiceApplicationBuilder()
        .bindings(new GuiceApplicationBuilderSpec.AModule)
        .disable(classOf[GuiceApplicationBuilderSpec.AModule])
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
        .load((env, conf) => Seq(new BuiltinModule, new I18nModule, bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A1]))
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
    }

    "set loaded modules directly" in {
      val injector = new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, bind[GuiceApplicationBuilderSpec.A].to[GuiceApplicationBuilderSpec.A1])
        .injector()

      injector.instanceOf[GuiceApplicationBuilderSpec.A] must beAnInstanceOf[GuiceApplicationBuilderSpec.A1]
    }

    "eagerly load singletons" in {
      new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, bind[GuiceApplicationBuilderSpec.C].to[GuiceApplicationBuilderSpec.C1])
        .eagerlyLoaded()
        .injector() must throwA[CreationException]
    }

    "work with built in modules and requireAtInjectOnConstructors" in {
      new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule)
        .requireAtInjectOnConstructors()
        .eagerlyLoaded()
        .injector() must not(throwA[CreationException])
    }

    "set lazy load singletons" in {
      val builder = new GuiceApplicationBuilder()
        .load(new BuiltinModule, new I18nModule, bind[GuiceApplicationBuilderSpec.C].to[GuiceApplicationBuilderSpec.C1])

      builder.injector() must throwAn[CreationException].not
      builder.injector().instanceOf[GuiceApplicationBuilderSpec.C] must throwAn[ProvisionException]
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

  class TestRoutes @Inject() (action: DefaultActionBuilder, request: RequestHeader) extends FakeRoutes({
    case ("GET", "/echo") => action(Ok(request.getQueryString("message") getOrElse ""))
  }, Router.empty)

  class RequestScopedRoutes @Inject() (injector: com.google.inject.Injector)
    extends RequestScopedRouter[TestRoutes](injector)

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

  class EagerlyLoadedException extends RuntimeException

}
