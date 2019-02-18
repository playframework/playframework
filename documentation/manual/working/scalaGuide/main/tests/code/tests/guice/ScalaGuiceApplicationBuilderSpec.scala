/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.tests.guice

import java.io.File
import java.net.URLClassLoader
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.api.test._

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
        .load(new play.api.inject.BuiltinModule, new play.api.i18n.I18nModule, new play.api.mvc.CookiesModule) // ###skip
        .loadConfig(Configuration.reference)                                                                   // ###skip
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .in(Environment(new File("path/to/app"), classLoader, Mode.Test))
        .build()
      // #set-environment

      application.path must_== new File("path/to/app")
      application.mode must_== Mode.Test
      application.classloader must be(classLoader)
    }

    "set environment values" in {
      val classLoader = new URLClassLoader(Array.empty)
      // #set-environment-values
      val application = new GuiceApplicationBuilder()
        .load(new play.api.inject.BuiltinModule, new play.api.i18n.I18nModule, new play.api.mvc.CookiesModule) // ###skip
        .loadConfig(Configuration.reference)                                                                   // ###skip
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .in(new File("path/to/app"))
        .in(Mode.Test)
        .in(classLoader)
        .build()
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
        .build()
      // #add-configuration

      application.configuration.get[Int]("a") must beEqualTo(1)
      application.configuration.get[Int]("b") must beEqualTo(2)
      application.configuration.get[String]("c") must beEqualTo("three")
      application.configuration.get[Int]("d") must beEqualTo(4)
      application.configuration.get[String]("e") must beEqualTo("five")
    }

    "override configuration" in {
      // #override-configuration
      val application = new GuiceApplicationBuilder()
        .loadConfig(env => Configuration.load(env))
        .build()
      // #override-configuration

      application.configuration.keys must not be empty
    }

    "add bindings" in {
      // #add-bindings
      val injector = new GuiceApplicationBuilder()
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .bindings(new ComponentModule)
        .bindings(bind[Component].to[DefaultComponent])
        .injector()
      // #add-bindings

      injector.instanceOf[Component] must beAnInstanceOf[DefaultComponent]
    }

    "override bindings" in {
      // #override-bindings
      val application = new GuiceApplicationBuilder()
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .configure("play.http.router" -> classOf[Routes].getName) // ###skip
        .bindings(new ComponentModule) // ###skip
        .overrides(bind[Component].to[MockComponent])
        .build()
      // #override-bindings

      running(application) {
        val Some(result) = route(application, FakeRequest(GET, "/"))
        contentAsString(result) must_== "mock"
      }
    }

    "load modules" in {
      // #load-modules
      val injector = new GuiceApplicationBuilder()
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .load(
          new play.api.inject.BuiltinModule,
          new play.api.i18n.I18nModule,
          new play.api.mvc.CookiesModule,
          bind[Component].to[DefaultComponent]
        )
        .injector()
      // #load-modules

      injector.instanceOf[Component] must beAnInstanceOf[DefaultComponent]
    }

    "disable modules" in {
      // #disable-modules
      val injector = new GuiceApplicationBuilder()
        .configure("play.http.filters" -> "play.api.http.NoHttpFilters") // ###skip
        .bindings(new ComponentModule) // ###skip
        .disable[ComponentModule]
        .injector()
      // #disable-modules

      injector.instanceOf[Component] must throwA[com.google.inject.ConfigurationException]
    }

    "injector builder" in {
      // #injector-builder
      val injector = new GuiceInjectorBuilder()
        .configure("key" -> "value")
        .bindings(new ComponentModule)
        .overrides(bind[Component].to[MockComponent])
        .injector()

      val component = injector.instanceOf[Component]
      // #injector-builder

      component must beAnInstanceOf[MockComponent]
    }

  }

}
