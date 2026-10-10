/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test.withoutbrowser

import scala.concurrent.Future

import play.api.mvc.Results
import play.api.test._

/**
 * play-test and play-specs2 only declare the test browser's dependencies (Selenide, Selenium, HtmlUnit) as optional,
 * apart from selenium-api. This project excludes them, like an application without play-test-browser.
 * PlaySpecification itself mixes in PlayRunners, which holds the browser constants and runners.
 */
class WithoutBrowserSpec extends PlaySpecification {
  "Without the test browser's dependencies" should {
    "not have the browsers on the classpath" in {
      Class.forName("org.openqa.selenium.htmlunit.HtmlUnitDriver") must throwA[ClassNotFoundException]
      Class.forName("org.openqa.selenium.firefox.FirefoxDriver") must throwA[ClassNotFoundException]
      Class.forName("com.codeborne.selenide.SelenideDriver") must throwA[ClassNotFoundException]
    }

    "allow using the helpers of PlaySpecification" in {
      val result = Future.successful(Results.Ok("hello"))
      status(result) must_== OK
      contentAsString(result) must_== "hello"
    }

    "allow using Helpers" in {
      Helpers.contentAsString(Future.successful(Results.Ok("hello"))) must_== "hello"
    }

    "allow extending PlayRunners without browser dependencies" in {
      val runners = new PlayRunners {}
      val app     = runners.baseApplicationBuilder.build()
      runners.running(app) {
        app.mode must_== play.api.Mode.Test
      }
    }

    "run WithApplication" in new WithApplication() {
      override def running() = {
        app.mode must_== play.api.Mode.Test
      }
    }

    "run WithServer" in new WithServer() {
      override def running() = {
        port must be_>(0)
      }
    }
  }
}
