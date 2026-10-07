/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import scala.jdk.CollectionConverters._

import com.codeborne.selenide.SelenideConfig
import org.openqa.selenium.Capabilities
import org.specs2.mutable._

class WebDriverFactorySpec extends Specification {

  sequential // reads and writes selenide.* system properties

  // The browser specific options, like "goog:chromeOptions" or "moz:firefoxOptions"
  private def browserOptions(options: Capabilities): java.util.Map[String, Any] =
    options
      .asMap()
      .asScala
      .collectFirst {
        case (key, value: java.util.Map[?, ?]) if key.endsWith("Options") =>
          value.asInstanceOf[java.util.Map[String, Any]]
      }
      .getOrElse(java.util.Map.of[String, Any]())

  private def arguments(options: Capabilities): Seq[String] =
    browserOptions(options).get("args") match {
      case args: java.util.List[?] => args.asScala.map(_.toString).toSeq
      case _                       => Seq.empty
    }

  private def binary(options: Capabilities): Option[String] =
    Option(browserOptions(options).get("binary")).map(_.toString)

  private val visible  = new SelenideConfig().headless(false).browserBinary(null)
  private val headless = new SelenideConfig().headless(true).browserBinary(null)

  "WebDriverFactory" should {
    "enable BiDi for Chrome, Edge and Firefox" in {
      WebDriverFactory.chromeOptions(visible).getCapability("webSocketUrl") must_== true
      WebDriverFactory.edgeOptions(visible).getCapability("webSocketUrl") must_== true
      WebDriverFactory.firefoxOptions(visible).getCapability("webSocketUrl") must_== true
    }

    "run Chrome, Edge and Firefox headless only if requested" in {
      arguments(WebDriverFactory.chromeOptions(headless)).contains("--headless=new") must beTrue
      arguments(WebDriverFactory.chromeOptions(visible)).contains("--headless=new") must beFalse
      arguments(WebDriverFactory.edgeOptions(headless)).contains("--headless=new") must beTrue
      arguments(WebDriverFactory.edgeOptions(visible)).contains("--headless=new") must beFalse
      arguments(WebDriverFactory.firefoxOptions(headless)).contains("-headless") must beTrue
      arguments(WebDriverFactory.firefoxOptions(visible)).contains("-headless") must beFalse
    }

    "use the browser binary only if configured" in {
      val config = new SelenideConfig().browserBinary("/opt/browser")
      binary(WebDriverFactory.chromeOptions(config)) must beSome("/opt/browser")
      binary(WebDriverFactory.edgeOptions(config)) must beSome("/opt/browser")
      binary(WebDriverFactory.firefoxOptions(config)) must beSome("/opt/browser")
      binary(WebDriverFactory.chromeOptions(visible)) must beNone
      binary(WebDriverFactory.firefoxOptions(visible)) must beNone
    }

    "read the settings from selenide.* system properties" in {
      val previous = Seq("selenide.headless", "selenide.browserBinary").map(key => key -> sys.props.get(key))
      try {
        sys.props("selenide.headless") = "true"
        sys.props("selenide.browserBinary") = "/opt/browser"
        val options = WebDriverFactory.chromeOptions(new SelenideConfig())
        (arguments(options).contains("--headless=new") must beTrue).and(binary(options) must beSome("/opt/browser"))
      } finally {
        previous.foreach {
          case (key, Some(value)) => sys.props(key) = value
          case (key, None)        => sys.props -= key
        }
      }
    }
  }
}
