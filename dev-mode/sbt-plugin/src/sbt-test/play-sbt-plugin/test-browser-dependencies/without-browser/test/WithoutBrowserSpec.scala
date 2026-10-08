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
