/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import akka.actor.ActorSystem
import com.google.inject.AbstractModule
import javax.inject.{ Inject, Provider }
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment, GlobalSettings }

object GuiceApplicationBuilderSpec extends Specification {

  "GuiceApplicationBuilder" should {

    "add bindings" in {
      val app = new GuiceApplicationBuilder()
        .bindings(
          new AModule,
          bind[B].to[B1])
        .build

      app.injector.instanceOf[A] must beAnInstanceOf[A1]
      app.injector.instanceOf[B] must beAnInstanceOf[B1]
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
      val app = new GuiceApplicationBuilder()
        .bindings(new AModule)
        .disable[play.api.libs.concurrent.AkkaModule]
        .disable(classOf[AModule])
        .build

      app.injector.instanceOf[ActorSystem] must throwA[com.google.inject.ConfigurationException]
      app.injector.instanceOf[A] must throwA[com.google.inject.ConfigurationException]
    }

    "set initial configuration loader" in {
      val extraConfig = Configuration("a" -> 1)
      val app = new GuiceApplicationBuilder()
        .loadConfig(env => Configuration.load(env) ++ extraConfig)
        .build

      app.configuration.getInt("a") must beSome(1)
    }

    "set global settings" in {
      val global = new GlobalSettings {
        override def configuration = Configuration("a" -> 1)
      }

      val app = new GuiceApplicationBuilder()
        .global(global)
        .build

      app.configuration.getInt("a") must beSome(1)
    }

    "set module loader" in {
      val app = new GuiceApplicationBuilder()
        .load((env, conf) => Seq(new BuiltinModule, bind[A].to[A1]))
        .build

      app.injector.instanceOf[A] must beAnInstanceOf[A1]
    }

    "set loaded modules directly" in {
      val app = new GuiceApplicationBuilder()
        .load(new BuiltinModule, bind[A].to[A1])
        .build

      app.injector.instanceOf[A] must beAnInstanceOf[A1]
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

}
