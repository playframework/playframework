/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.PlaySpecification
import play.api.test.WithBrowser

/**
 * Tests that the specs2 [[WithBrowser]] scope accepts the browser constants as web driver class.
 */
class WithBrowserSpec extends PlaySpecification {
  "WithBrowser" should {
    "accept a browser constant as web driver class" in new WithBrowser(HTMLUNIT, GuiceApplicationBuilder().build(), 0) {
      override def running() = {
        webDriver must beAnInstanceOf[HtmlUnitDriver]
      }
    }

    "accept a browser constant with an explicit web driver type" in new WithBrowser[HtmlUnitDriver](
      HTMLUNIT,
      GuiceApplicationBuilder().build(),
      0
    ) {
      override def running() = {
        webDriver must beAnInstanceOf[HtmlUnitDriver]
      }
    }
  }
}
