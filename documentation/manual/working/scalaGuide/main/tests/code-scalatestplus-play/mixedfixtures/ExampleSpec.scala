/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package org.scalatestplus.play.examples.mixedfixtures

import play.api.test._
import org.scalatest._
import org.scalatestplus.play._
import play.api.{Play, Application}
import play.api.mvc._

// #scalafunctionaltest-mixedfixtures
// MixedPlaySpec already mixes in MixedFixtures
class ExampleSpec extends MixedPlaySpec {

  // Some helper methods
  def fakeApp[A](elems: (String, String)*) = FakeApplication(additionalConfiguration = Map(elems:_*),
    withRoutes = {
      case ("GET", "/testing") =>
        Action(
          Results.Ok(
            "<html>" +
              "<head><title>Test Page</title></head>" +
              "<body>" +
              "<input type='button' name='b' value='Click Me' onclick='document.title=\"scalatest\"' />" +
              "</body>" +
              "</html>"
          ).as("text/html")
        )
    })
  def getConfig(key: String)(implicit app: Application) = app.configuration.getString(key)

  // If a test just needs a FakeApplication, use "new App":
  "The App function" must {
    "provide a FakeApplication" in new App(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new App(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new App(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
  }

  // If a test needs a FakeApplication and running TestServer, use "new Server":
  "The Server function" must {
    "provide a FakeApplication" in new Server(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new Server(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new Server(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new Server {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
  }

  // If a test needs a FakeApplication, running TestServer, and Selenium
  // HtmlUnit driver use "new HtmlUnit":
  "The HtmlUnit function" must {
    "provide a FakeApplication" in new HtmlUnit(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new HtmlUnit(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new HtmlUnit(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new HtmlUnit {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide a web driver" in new HtmlUnit(fakeApp()) {
      go to ("http://localhost:" + port + "/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }

  // If a test needs a FakeApplication, running TestServer, and Selenium
  // Firefox driver use "new Firefox":
  "The Firefox function" must {
    "provide a FakeApplication" in new Firefox(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new Firefox(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new Firefox(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new Firefox {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide a web driver" in new Firefox(fakeApp()) {
      go to ("http://localhost:" + port + "/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }

  // If a test needs a FakeApplication, running TestServer, and Selenium
  // Safari driver use "new Safari":
  "The Safari function" must {
    "provide a FakeApplication" in new Safari(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new Safari(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new Safari(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new Safari {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide a web driver" in new Safari(fakeApp()) {
      go to ("http://localhost:" + port + "/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }

  // If a test needs a FakeApplication, running TestServer, and Selenium
  // Chrome driver use "new Chrome":
  "The Chrome function" must {
    "provide a FakeApplication" in new Chrome(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new Chrome(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new Chrome(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new Chrome {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide a web driver" in new Chrome(fakeApp()) {
      go to ("http://localhost:" + port + "/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }

  // If a test needs a FakeApplication, running TestServer, and Selenium
  // InternetExplorer driver use "new InternetExplorer":
  "The InternetExplorer function" must {
    "provide a FakeApplication" in new InternetExplorer(fakeApp("ehcacheplugin" -> "disabled")) {
      app.configuration.getString("ehcacheplugin") mustBe Some("disabled")
    }
    "make the FakeApplication available implicitly" in new InternetExplorer(fakeApp("ehcacheplugin" -> "disabled")) {
      getConfig("ehcacheplugin") mustBe Some("disabled")
    }
    "start the FakeApplication" in new InternetExplorer(fakeApp("ehcacheplugin" -> "disabled")) {
      Play.maybeApplication mustBe Some(app)
    }
    import Helpers._
    "send 404 on a bad request" in new InternetExplorer {
      import java.net._
      val url = new URL("http://localhost:" + port + "/boom")
      val con = url.openConnection().asInstanceOf[HttpURLConnection]
      try con.getResponseCode mustBe 404
      finally con.disconnect()
    }
    "provide a web driver" in new InternetExplorer(fakeApp()) {
      go to ("http://localhost:" + port + "/testing")
      pageTitle mustBe "Test Page"
      click on find(name("b")).value
      eventually { pageTitle mustBe "scalatest" }
    }
  }

  // If a test does not need any special fixtures, just 
  // write "in { () => ..."
  "Any old thing" must {
    "be doable without much boilerplate" in { () =>
       1 + 1 mustEqual 2
     }
  }
}
// #scalafunctionaltest-mixedfixtures
