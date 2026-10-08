/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._

class WithBrowserSpec extends PlaySpecification {
  "Tests with play-test-browser" should {
    "use the test browser" in new WithBrowser(HTMLUNIT, GuiceApplicationBuilder().build(), 0) {
      override def running() = {
        browser.goTo("/")
        browser.el("#greeting").getText must equalTo("Hello browser")
      }
    }
  }
}
