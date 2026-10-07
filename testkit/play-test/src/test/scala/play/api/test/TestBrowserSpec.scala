/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.test

import scala.jdk.CollectionConverters._

import com.codeborne.selenide.impl.Plugins
import com.codeborne.selenide.impl.ScreenShotLaboratory
import com.codeborne.selenide.CollectionCondition
import com.codeborne.selenide.Condition
import org.specs2.mutable._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Application

class TestBrowserSpec extends Specification {

  sequential

  private val index =
    """<html>
      |<head><title>Index</title></head>
      |<body>
      |  <div id="title">Hello Guest</div>
      |  <ul><li>One</li><li>Two</li></ul>
      |  <a id="login" href="/login">login</a>
      |  <form id="form" action="/submit" method="post">
      |    <input type="text" name="email">
      |    <input type="text" name="items[0]">
      |  </form>
      |</body>
      |</html>""".stripMargin

  private def page(content: String) = s"""<html><body><div id="result">$content</div></body></html>"""

  private def application(): Application =
    GuiceApplicationBuilder()
      .appRoutes { app =>
        val Action                                             = app.injector.instanceOf[DefaultActionBuilder]
        val parse                                              = app.injector.instanceOf[PlayBodyParsers]
        val routes: PartialFunction[(String, String), Handler] = {
          case ("GET", "/")         => Action(Ok(index).as("text/html").withCookies(Cookie("flavour", "chocolate")))
          case ("GET", "/login")    => Action(Ok(page("Login")).as("text/html"))
          case ("GET", "/app/page") => Action(Ok(page("App page")).as("text/html"))
          case ("POST", "/submit")  =>
            Action(parse.formUrlEncoded) { request =>
              val fields = request.body.toSeq.sortBy(_._1).map { case (k, v) => s"$k=${v.mkString(",")}" }
              Ok(page(fields.mkString("&"))).as("text/html")
            }
        }
        routes
      }
      .build()

  private def withBaseUrl[T](baseUrl: Int => Option[String])(block: (TestBrowser, Int) => T): T = {
    val server = TestServer(0, application())
    Helpers.running(server) {
      val port    = server.runningHttpPort.get
      val browser = TestBrowser.default(baseUrl(port))
      try block(browser, port)
      finally browser.quit()
    }
  }

  private def withBrowser[T](block: TestBrowser => T): T =
    withBaseUrl(port => Some(s"http://localhost:$port"))((browser, _) => block(browser))

  "TestBrowser" should {
    "resolve relative urls against the base url, with or without a leading slash" in withBrowser { browser =>
      browser.goTo("login")
      browser.el("#result").text() must_== "Login"
      browser.url must_== "login"

      browser.goTo("/")
      browser.url must_== ""
      browser.goTo("/login")
      browser.url() must_== "login"
    }

    "resolve relative urls against a base url with a path" in {
      withBaseUrl(port => Some(s"http://localhost:$port/app")) { (browser, _) =>
        browser.goTo("page")
        browser.el("#result").text() must_== "App page"
        browser.url must_== "page"
      }
    }

    "return absolute urls when there is no base url" in {
      withBaseUrl(_ => None) { (browser, port) =>
        browser.goTo(s"http://localhost:$port/login")
        browser.url must_== s"http://localhost:$port/login"
        browser.getBaseUrl must beNull
      }
    }

    "not open relative urls when there is no base url" in {
      withBaseUrl(_ => None) { (browser, _) =>
        browser.goTo("/login") must throwAn[IllegalArgumentException]
      }
    }

    "use the base url of the Selenide configuration" in withBaseUrl(_ => None) { (browser, port) =>
      browser.selenideConfig().baseUrl(s"http://localhost:$port/app")
      browser.getBaseUrl must_== s"http://localhost:$port/app"
      browser.goTo("page")
      browser.el("#result").text() must_== "App page"
      browser.url must_== "page"
    }

    "find single elements with el and all elements with $ and find" in withBrowser { browser =>
      browser.goTo("/")
      browser.el("#title").text() must_== "Hello Guest"
      browser.el("li").text() must_== "One"
      browser.$("li").size() must_== 2
      browser.$("li").texts().asScala must_== Seq("One", "Two")
      browser.find("li").get(1).text() must_== "Two"
      browser.$("li").shouldHave(CollectionCondition.size(2))
      browser.el("#title").shouldHave(Condition.text("Hello"))
      browser.pageSource must contain("Hello Guest")
    }

    "click elements" in withBrowser { browser =>
      browser.goTo("/")
      browser.el("#login").click()
      browser.url must_== "login"
    }

    "submit forms" in withBrowser { browser =>
      browser.goTo("/")
      browser.submit("#form", "email" -> "coco@example.com", "items[0]" -> "first")
      browser.el("#result").text() must_== "email=coco@example.com&items[0]=first"
    }

    "execute scripts" in withBrowser { browser =>
      browser.goTo("/")
      browser.executeScript("return document.title") must_== "Index"
      browser.executeScript("return arguments[0] + 1", Int.box(41)) must_== 42L
    }

    "read cookies" in withBrowser { browser =>
      browser.goTo("/")
      browser.getCookie("flavour").getValue must_== "chocolate"
      browser.getCookies.asScala.map(_.getName) must contain("flavour")
    }

    "not record screenshots in Selenide's global screenshot laboratory" in withBrowser { browser =>
      val global = Plugins.inject(classOf[ScreenShotLaboratory])
      val before = global.screenshots().size()
      browser.goTo("/")
      browser.selenide.screenshot("TestBrowserSpec")
      global.screenshots().size() must_== before
    }
  }
}
