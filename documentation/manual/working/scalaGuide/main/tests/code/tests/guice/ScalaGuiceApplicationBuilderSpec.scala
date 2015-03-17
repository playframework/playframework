/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.tests.guice

import java.io.File
import java.net.URLClassLoader
import play.api.{ Configuration, Environment, Mode }
import play.api.test._
import play.api.test.Helpers._

// #builder-imports
import play.api.inject.guice.GuiceApplicationBuilder
// #builder-imports

// #bind-imports
import play.api.inject.bind
// #bind-imports

// #injector-imports
import play.api.inject.guice.GuiceInjectorBuilder
// #injector-imports

class ScalaGuiceApplicationBuilderSpec extends PlaySpecification {

  "Scala GuiceApplicationBuilder" should {

    "set environment" in {
      val classLoader = new URLClassLoader(Array.empty)
      // #set-environment
      val application = new GuiceApplicationBuilder()
        .load(new play.api.inject.BuiltinModule) // ###skip
        .loadConfig(Configuration.reference) // ###skip
        .in(Environment(new File("path/to/app"), classLoader, Mode.Test))
        .build
      // #set-environment

      application.path must_== new File("path/to/app")
      application.mode must_== Mode.Test
      application.classloader must be(classLoader)
    }

    "set environment values" in {
      val classLoader = new URLClassLoader(Array.empty)
      // #set-environment-values
      val application = new GuiceApplicationBuilder()
        .load(new play.api.inject.BuiltinModule) // ###skip
        .loadConfig(Configuration.reference) // ###skip
        .in(new File("path/to/app"))
        .in(Mode.Test)
        .in(classLoader)
        .build
      // #set-environment-values

      application.path must_== new File("path/to/app")
      application.mode must_== Mode.Test
      application.classloader must be(classLoader)
    }

    "add configuration" in {
      // #add-configuration
      val application = new GuiceApplicationBuilder()
        .configure(Configuration("a" -> 1))
        .configure(Map("b" -> 2, "c" -> "three"))
        .configure("d" -> 4, "e" -> "five")
        .build
      // #add-configuration

      application.configuration.getInt("a") must beSome(1)
      application.configuration.getInt("b") must beSome(2)
      application.configuration.getString("c") must beSome("three")
      application.configuration.getInt("d") must beSome(4)
      application.configuration.getString("e") must beSome("five")
    }

    "override configuration" in {
      // #override-configuration
      val application = new GuiceApplicationBuilder()
        .loadConfig(env => Configuration.load(env))
        .build
      // #override-configuration

      application.configuration.keys must not be empty
    }

    "add bindings" in {
      // #add-bindings
      val application = new GuiceApplicationBuilder()
        .bindings(new ComponentModule)
        .bindings(bind[Component].to[DefaultComponent])
        .build
      // #add-bindings

      application.injector.instanceOf[Component] must beAnInstanceOf[DefaultComponent]
    }

    "override bindings" in {
      // #override-bindings
      val application = new GuiceApplicationBuilder()
        .configure("play.http.router" -> classOf[Routes].getName) // ###skip
        .bindings(new ComponentModule) // ###skip
        .overrides(bind[Component].to[MockComponent])
        .build
      // #override-bindings

      running(application) {
        val Some(result) = route(FakeRequest(GET, "/"))
        contentAsString(result) must_== "mock"
      }
    }

    "load modules" in {
      // #load-modules
      val application = new GuiceApplicationBuilder()
        .load(
          new play.api.inject.BuiltinModule,
          bind[Component].to[DefaultComponent]
        ).build
      // #load-modules

      application.injector.instanceOf[Component] must beAnInstanceOf[DefaultComponent]
    }

    "disable modules" in {
      // #disable-modules
      val application = new GuiceApplicationBuilder()
        .bindings(new ComponentModule) // ###skip
        .disable[ComponentModule]
        .build
      // #disable-modules

      application.injector.instanceOf[Component] must throwA[com.google.inject.ConfigurationException]
    }

    "injector builder" in {
      // #injector-builder
      val injector = new GuiceInjectorBuilder()
        .configure("key" -> "value")
        .bindings(new ComponentModule)
        .overrides(bind[Component].to[MockComponent])
        .build

      val component = injector.instanceOf[Component]
      // #injector-builder

      component must beAnInstanceOf[MockComponent]
    }

  }

}
