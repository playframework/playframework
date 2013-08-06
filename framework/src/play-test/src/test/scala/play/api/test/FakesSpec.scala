/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.test

import org.specs2.mutable.Specification

import play.api.mvc._
import play.api.test.Helpers._
import play.api.{GlobalSettings, Plugin, DefaultInjectionProvider}

object FakesSpec extends Specification {

  "FakeApplication" should {

    "allow adding routes inline" in {
      val app = new FakeApplication(
        withRoutes = {
          case ("GET", "/inline") => Action { Results.Ok("inline route") }
        }
      )
      running(app) {
        val result = route(app, FakeRequest("GET", "/inline"))
        result must beSome
        contentAsString(result.get) must_== "inline route"
        route(app, FakeRequest("GET", "/foo")) must beNone
      }
    }

    "allow customizing the injection provider" in {
      val myPlugin = new TestPlugin()
      class MyGlobalSettings extends GlobalSettings
      val app = new FakeApplication() {
        override lazy val injectionProvider = new DefaultInjectionProvider(this) {
          override lazy val plugins = Seq(myPlugin)
          override lazy val global = new MyGlobalSettings
        }
      }
      running(app) {
        app.plugin[TestPlugin] must beSome(myPlugin)
        app.plugin[TestPlugin].get.started must beTrue
        app.global.isInstanceOf[MyGlobalSettings] must beTrue
      }
    }
  }

  class TestPlugin extends Plugin {
    override val enabled = true
    var started = false
    override def onStart(): Unit = {
      started = true
      super.onStart()
    }
  }
}
