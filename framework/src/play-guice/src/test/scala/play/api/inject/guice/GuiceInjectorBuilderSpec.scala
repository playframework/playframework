/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.inject
package guice

import java.io.File
import java.net.URLClassLoader
import java.util.Collections
import java.util.function.Supplier

import com.google.inject.AbstractModule
import com.typesafe.config.Config
import org.specs2.mutable.Specification
import play.{ Environment => JavaEnvironment }
import play.api.inject._
import play.api.{ Configuration, Environment, Mode }
import play.inject.{ Module => JavaModule }

class GuiceInjectorBuilderSpec extends Specification {

  "GuiceInjectorBuilder" should {

    "set environment with Scala" in {
      setEnvironment(new GuiceInjectorBuilderSpec.EnvironmentModule)
    }

    "set environment with Java" in {
      setEnvironment(new GuiceInjectorBuilderSpec.JavaEnvironmentModule)
    }

    def setEnvironment(environmentModule: Module) = {
      val env = new GuiceInjectorBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .bindings(environmentModule)
        .injector().instanceOf[Environment]

      env.mode must_== Mode.Dev
    }

    "set environment values with Scala" in {
      setEnvironmentValues(new GuiceInjectorBuilderSpec.EnvironmentModule)
    }

    "set environment values with Java" in {
      setEnvironmentValues(new GuiceInjectorBuilderSpec.JavaEnvironmentModule)
    }

    def setEnvironmentValues(environmentModule: Module) = {
      val classLoader = new URLClassLoader(Array.empty)
      val env = new GuiceInjectorBuilder()
        .in(new File("test"))
        .in(Mode.Dev)
        .in(classLoader)
        .bindings(environmentModule)
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

    "set configuration with Scala" in {
      setConfiguration(new GuiceInjectorBuilderSpec.ConfigurationModule)
    }

    "set configuration with Java" in {
      setConfiguration(new GuiceInjectorBuilderSpec.JavaConfigurationModule)
    }

    def setConfiguration(configurationModule: Module) = {
      val conf = new GuiceInjectorBuilder()
        .configure(Configuration("a" -> 1))
        .configure(Map("b" -> 2))
        .configure("c" -> 3)
        .configure("d.1" -> 4, "d.2" -> 5)
        .bindings(configurationModule)
        .injector().instanceOf[Configuration]

      conf.subKeys must contain(allOf("a", "b", "c", "d"))
      conf.get[Int]("a") must_== 1
      conf.get[Int]("b") must_== 2
      conf.get[Int]("c") must_== 3
      conf.get[Int]("d.1") must_== 4
      conf.get[Int]("d.2") must_== 5
    }

    "support various bindings with Scala" in {
      supportVariousBindings(new GuiceInjectorBuilderSpec.EnvironmentModule, new GuiceInjectorBuilderSpec.ConfigurationModule)
    }

    "support various bindings with Java" in {
      supportVariousBindings(new GuiceInjectorBuilderSpec.JavaEnvironmentModule, new GuiceInjectorBuilderSpec.JavaConfigurationModule)
    }

    def supportVariousBindings(environmentModule: Module, configurationModule: Module) = {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          environmentModule,
          Seq(configurationModule),
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

    "override bindings with Scala" in {
      overrideBindings(new GuiceInjectorBuilderSpec.EnvironmentModule, new GuiceInjectorBuilderSpec.ConfigurationModule)
    }

    "override bindings with Java" in {
      overrideBindings(new GuiceInjectorBuilderSpec.JavaEnvironmentModule, new GuiceInjectorBuilderSpec.JavaConfigurationModule)
    }

    def overrideBindings(environmentModule: Module, configurationModule: Module) = {
      val injector = new GuiceInjectorBuilder()
        .in(Mode.Dev)
        .configure("a" -> 1)
        .bindings(
          environmentModule,
          configurationModule)
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

    "disable modules with Scala" in {
      disableModules(new GuiceInjectorBuilderSpec.EnvironmentModule, new GuiceInjectorBuilderSpec.ConfigurationModule)
    }

    "disable modules with Java" in {
      disableModules(new GuiceInjectorBuilderSpec.JavaEnvironmentModule, new GuiceInjectorBuilderSpec.JavaConfigurationModule)
    }

    def disableModules(environmentModule: Module, configurationModule: Module) = {
      val injector = new GuiceInjectorBuilder()
        .bindings(
          environmentModule,
          configurationModule,
          new GuiceInjectorBuilderSpec.AModule,
          new GuiceInjectorBuilderSpec.BModule,
          bind[GuiceInjectorBuilderSpec.C].to[GuiceInjectorBuilderSpec.C1],
          bind[GuiceInjectorBuilderSpec.D] to new GuiceInjectorBuilderSpec.D1)
        .disable[GuiceInjectorBuilderSpec.EnvironmentModule]
        .disable[GuiceInjectorBuilderSpec.JavaEnvironmentModule]
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

  class JavaEnvironmentModule extends JavaModule {
    override def bindings(environment: JavaEnvironment, config: Config) = Collections.singletonList(JavaModule.bindClass(classOf[Environment]).to(new Supplier[Environment] {
      override def get(): Environment = environment.asScala()
    }))
  }

  class JavaConfigurationModule extends JavaModule {
    override def bindings(environment: JavaEnvironment, config: Config) = Collections.singletonList(JavaModule.bindClass(classOf[Configuration]).to(new Supplier[Configuration] {
      override def get(): Configuration = Configuration(config)
    }))
  }

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
