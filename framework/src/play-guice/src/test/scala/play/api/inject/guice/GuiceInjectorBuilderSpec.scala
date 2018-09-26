/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject
package guice

import java.io.File
import java.net.URLClassLoader

import com.google.inject.AbstractModule
import org.specs2.mutable.Specification
import play.api.inject._
import play.api.{ Configuration, Environment, Mode, inject }

class GuiceInjectorBuilderSpec extends Specification {

  "GuiceInjectorBuilder" should {

    "set environment" in {
      val env = new GuiceInjectorBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .bindings(new GuiceInjectorBuilderSpec.EnvironmentModule)
        .injector().instanceOf[Environment]

      env.mode must_== Mode.Dev
    }

    "set environment values" in {
      val classLoader = new URLClassLoader(Array.empty)
      val env = new GuiceInjectorBuilder()
        .in(new File("test"))
        .in(Mode.Dev)
        .in(classLoader)
        .bindings(new GuiceInjectorBuilderSpec.EnvironmentModule)
        .injector().instanceOf[Environment]

      env.rootPath must_== new File("test")
      env.mode must_== Mode.Dev
      env.classLoader must be(classLoader)
    }

    "use the right ClassLoader when injecting" in {
      val classLoader = new URLClassLoader(Array.empty)
      val classLoaderAware = new GuiceInjectorBuilder()
        .in(classLoader)
        .bindings(bind[GuiceInjectorBuilderSpec.ClassLoaderAware].toSelf)
        .injector().instanceOf[GuiceInjectorBuilderSpec.ClassLoaderAware]

      classLoaderAware.constructionClassLoader must_== classLoader
    }

    "set configuration" in {
      val conf = new GuiceInjectorBuilder()
        .configure(Configuration("a" -> 1))
        .configure(Map("b" -> 2))
        .configure("c" -> 3)
        .configure("d.1" -> 4, "d.2" -> 5)
        .bindings(new GuiceInjectorBuilderSpec.ConfigurationModule)
        .injector().instanceOf[Configuration]

      conf.subKeys must contain(allOf("a", "b", "c", "d"))
      conf.get[Int]("a") must_== 1
      conf.get[Int]("b") must_== 2
      conf.get[Int]("c") must_== 3
      conf.get[Int]("d.1") must_== 4
      conf.get[Int]("d.2") must_== 5
    }

    "support various bindings" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          new GuiceInjectorBuilderSpec.EnvironmentModule,
          Seq(new GuiceInjectorBuilderSpec.ConfigurationModule),
          new GuiceInjectorBuilderSpec.AModule,
          Seq(new GuiceInjectorBuilderSpec.BModule))
        .bindings(
          bind[GuiceInjectorBuilderSpec.C].to[GuiceInjectorBuilderSpec.C1],
          Seq(bind[GuiceInjectorBuilderSpec.D].to[GuiceInjectorBuilderSpec.D1]))
        .injector()

      injector.instanceOf[Environment] must beAnInstanceOf[Environment]
      injector.instanceOf[Configuration] must beAnInstanceOf[Configuration]
      injector.instanceOf[GuiceInjectorBuilderSpec.A] must beAnInstanceOf[GuiceInjectorBuilderSpec.A1]
      injector.instanceOf[GuiceInjectorBuilderSpec.B] must beAnInstanceOf[GuiceInjectorBuilderSpec.B1]
      injector.instanceOf[GuiceInjectorBuilderSpec.C] must beAnInstanceOf[GuiceInjectorBuilderSpec.C1]
      injector.instanceOf[GuiceInjectorBuilderSpec.D] must beAnInstanceOf[GuiceInjectorBuilderSpec.D1]
    }

    "override bindings" in {
      val injector = new GuiceInjectorBuilder()
        .in(Mode.Dev)
        .configure("a" -> 1)
        .bindings(
          new GuiceInjectorBuilderSpec.EnvironmentModule,
          new GuiceInjectorBuilderSpec.ConfigurationModule)
        .overrides(
          bind[Environment] to Environment.simple(),
          new GuiceInjectorBuilderSpec.SetConfigurationModule(Configuration("b" -> 2)))
        .injector()

      val env = injector.instanceOf[Environment]
      val conf = injector.instanceOf[Configuration]
      env.mode must_== Mode.Test
      conf.has("a") must beFalse
      conf.get[Int]("b") must_== 2
    }

    "disable modules" in {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          new GuiceInjectorBuilderSpec.EnvironmentModule,
          new GuiceInjectorBuilderSpec.ConfigurationModule,
          new GuiceInjectorBuilderSpec.AModule,
          new GuiceInjectorBuilderSpec.BModule,
          bind[GuiceInjectorBuilderSpec.C].to[GuiceInjectorBuilderSpec.C1],
          bind[GuiceInjectorBuilderSpec.D] to new GuiceInjectorBuilderSpec.D1)
        .disable[GuiceInjectorBuilderSpec.EnvironmentModule]
        .disable(classOf[GuiceInjectorBuilderSpec.AModule], classOf[GuiceInjectorBuilderSpec.CModule]) // C won't be disabled
        .injector()

      injector.instanceOf[Environment] must throwA[com.google.inject.ConfigurationException]
      injector.instanceOf[GuiceInjectorBuilderSpec.A] must throwA[com.google.inject.ConfigurationException]

      injector.instanceOf[Configuration] must beAnInstanceOf[Configuration]
      injector.instanceOf[GuiceInjectorBuilderSpec.B] must beAnInstanceOf[GuiceInjectorBuilderSpec.B1]
      injector.instanceOf[GuiceInjectorBuilderSpec.C] must beAnInstanceOf[GuiceInjectorBuilderSpec.C1]
      injector.instanceOf[GuiceInjectorBuilderSpec.D] must beAnInstanceOf[GuiceInjectorBuilderSpec.D1]
    }

    "configure binder" in {
      val injector = new GuiceInjectorBuilder()
        .requireExplicitBindings()
        .bindings(
          bind[GuiceInjectorBuilderSpec.A].to[GuiceInjectorBuilderSpec.A1],
          bind[GuiceInjectorBuilderSpec.B].to[GuiceInjectorBuilderSpec.B1]
        )
        .injector()
      injector.instanceOf[GuiceInjectorBuilderSpec.A] must beAnInstanceOf[GuiceInjectorBuilderSpec.A1]
      injector.instanceOf[GuiceInjectorBuilderSpec.B] must beAnInstanceOf[GuiceInjectorBuilderSpec.B1]
      injector.instanceOf[GuiceInjectorBuilderSpec.B1] must throwA[com.google.inject.ConfigurationException]
      injector.instanceOf[GuiceInjectorBuilderSpec.C1] must throwA[com.google.inject.ConfigurationException]
    }

  }

}

object GuiceInjectorBuilderSpec {

  class EnvironmentModule extends SimpleModule((env, _) => Seq(bind[Environment] to env))

  class ConfigurationModule extends SimpleModule((_, conf) => Seq(bind[Configuration] to conf))

  class SetConfigurationModule(conf: Configuration) extends AbstractModule {
    override def configure() = bind(classOf[Configuration]) toInstance conf
  }

  class ClassLoaderAware {
    // This is the value of the Thread's context ClassLoader at the time the object is constructed
    val constructionClassLoader: ClassLoader = Thread.currentThread.getContextClassLoader
  }

  trait A
  class A1 extends A

  class AModule extends AbstractModule {
    override def configure() = bind(classOf[A]) to classOf[A1]
  }

  trait B
  class B1 extends B

  class BModule extends AbstractModule {
    override def configure() = bind(classOf[B]) to classOf[B1]
  }

  trait C
  class C1 extends C

  class CModule extends AbstractModule {
    override def configure() = bind(classOf[C]) to classOf[C1]
  }

  trait D
  class D1 extends D

}
