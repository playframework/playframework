/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject
package guice

import com.google.inject.AbstractModule
import java.io.File
import java.net.URLClassLoader
import org.specs2.mutable.Specification
import play.api.{ Configuration, Environment, Mode }

object GuiceInjectorBuilderSpec extends Specification {

  "GuiceInjectorBuilder" should {

    "set environment" in {
      val env = new GuiceInjectorBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .bindings(new EnvironmentModule)
        .injector.instanceOf[Environment]

      env.mode must_== Mode.Dev
    }

    "set environment values" in {
      val classLoader = new URLClassLoader(Array.empty)
      val env = new GuiceInjectorBuilder()
        .in(new File("test"))
        .in(Mode.Dev)
        .in(classLoader)
        .bindings(new EnvironmentModule)
        .injector.instanceOf[Environment]

      env.rootPath must_== new File("test")
      env.mode must_== Mode.Dev
      env.classLoader must be(classLoader)
    }

    "set configuration" in {
      val conf = new GuiceInjectorBuilder()
        .configure(Configuration("a" -> 1))
        .configure(Map("b" -> 2))
        .configure("c" -> 3)
        .configure("d.1" -> 4, "d.2" -> 5)
        .bindings(new ConfigurationModule)
        .injector.instanceOf[Configuration]

      conf.subKeys must contain(allOf("a", "b", "c", "d"))
      conf.getInt("a") must beSome(1)
      conf.getInt("b") must beSome(2)
      conf.getInt("c") must beSome(3)
      conf.getInt("d.1") must beSome(4)
      conf.getInt("d.2") must beSome(5)
    }

    "support various bindings" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          new EnvironmentModule,
          Seq(new ConfigurationModule),
          new AModule,
          Seq(new BModule))
        .bindings(
          bind[C].to[C1],
          Seq(bind[D].to[D1]))
        .injector

      injector.instanceOf[Environment] must beAnInstanceOf[Environment]
      injector.instanceOf[Configuration] must beAnInstanceOf[Configuration]
      injector.instanceOf[A] must beAnInstanceOf[A1]
      injector.instanceOf[B] must beAnInstanceOf[B1]
      injector.instanceOf[C] must beAnInstanceOf[C1]
      injector.instanceOf[D] must beAnInstanceOf[D1]
    }

    "override bindings" in {
      val injector = new GuiceInjectorBuilder()
        .in(Mode.Dev)
        .configure("a" -> 1)
        .bindings(
          new EnvironmentModule,
          new ConfigurationModule)
        .overrides(
          bind[Environment] to Environment.simple(),
          new SetConfigurationModule(Configuration("b" -> 2)))
        .injector

      val env = injector.instanceOf[Environment]
      val conf = injector.instanceOf[Configuration]
      env.mode must_== Mode.Test
      conf.getInt("a") must beNone
      conf.getInt("b") must beSome(2)
    }

    "disable modules" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          new EnvironmentModule,
          new ConfigurationModule,
          new AModule,
          new BModule,
          bind[C].to[C1],
          bind[D] to new D1)
        .disable[EnvironmentModule]
        .disable(classOf[AModule], classOf[CModule]) // C won't be disabled
        .injector

      injector.instanceOf[Environment] must throwA[com.google.inject.ConfigurationException]
      injector.instanceOf[A] must throwA[com.google.inject.ConfigurationException]

      injector.instanceOf[Configuration] must beAnInstanceOf[Configuration]
      injector.instanceOf[B] must beAnInstanceOf[B1]
      injector.instanceOf[C] must beAnInstanceOf[C1]
      injector.instanceOf[D] must beAnInstanceOf[D1]
    }

  }

  class EnvironmentModule extends Module {
    def bindings(env: Environment, conf: Configuration) = Seq(
      bind[Environment] to env
    )
  }

  class ConfigurationModule extends Module {
    def bindings(env: Environment, conf: Configuration) = Seq(
      bind[Configuration] to conf
    )
  }

  class SetConfigurationModule(conf: Configuration) extends AbstractModule {
    def configure = bind(classOf[Configuration]) toInstance conf
  }

  trait A
  class A1 extends A

  class AModule extends AbstractModule {
    def configure = bind(classOf[A]) to classOf[A1]
  }

  trait B
  class B1 extends B

  class BModule extends AbstractModule {
    def configure = bind(classOf[B]) to classOf[B1]
  }

  trait C
  class C1 extends C

  class CModule extends AbstractModule {
    def configure = bind(classOf[C]) to classOf[C1]
  }

  trait D
  class D1 extends D

}
