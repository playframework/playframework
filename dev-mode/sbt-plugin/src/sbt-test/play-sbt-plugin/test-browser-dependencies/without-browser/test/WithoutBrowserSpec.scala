/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.test._

class WithoutBrowserSpec extends PlaySpecification {
  "Tests without play-test-browser" should {
    "not get the test browser's dependencies" in {
      Class.forName("com.codeborne.selenide.SelenideDriver") must throwA[ClassNotFoundException]
      Class.forName("org.openqa.selenium.htmlunit.HtmlUnitDriver") must throwA[ClassNotFoundException]
      Class.forName("org.openqa.selenium.firefox.FirefoxDriver") must throwA[ClassNotFoundException]
    }

    "allow building pairs with ->" in {
      // Scala 3.10 failed to compile this without Selenide while TestBrowser.submit was defined in Scala
      ("email" -> "user@example.com")._1 must equalTo("email")
    }

    "allow extending PlayRunners without browser dependencies" in {
      val runners = new PlayRunners {}
      val app     = runners.baseApplicationBuilder.build()
      runners.running(app) {
        app.mode must equalTo(play.api.Mode.Test)
      }
    }

    "run an application" in new WithApplication() {
      override def running() = {
        app.mode must equalTo(play.api.Mode.Test)
      }
    }

    "run a server" in new WithServer(port = 0) {
      override def running() = {
        port must be_>(0)
      }
    }
  }
}
