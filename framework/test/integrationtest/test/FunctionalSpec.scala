package test

import play.api.test._
import play.api.libs.ws._
import models._
import models.Protocol._
import java.util.Calendar
import java.util.Locale
import play.api.libs.iteratee.Iteratee
import play.api.libs.ws.ResponseHeaders
import scala.concurrent.ExecutionContext.Implicits.global

class FunctionalSpec extends PlaySpecification {
  "an Application" should {
    

    "charset should be defined" in new WithServer() {
      val h = await(WS.url("http://localhost:" + port + "/public/stylesheets/main.css").get)
      h.header("Content-Type").get must equalTo("text/css; charset=utf-8")
    }
    "call onClose for Ok.sendFile responses" in new WithBrowser() {
      import java.io.File
      def file = new File("onClose.tmp")
      file.createNewFile()
      file.exists() must equalTo(true)

      browser.goTo("/onCloseSendFile/" + file.getCanonicalPath)
      Thread.sleep(1000)
      file.exists() must equalTo(false)
    }

    "pass functional test with two browsers" in new WithBrowser() {
      browser.goTo("/")
      browser.pageSource must contain("Hello world")
    }
    "pass functional test" in new WithBrowser() {
      val hp = WS.url("http://localhost:" + port + "/jsonWithContentType").
        withHeaders("Accept"-> "application/json").
        get{ header: ResponseHeaders =>
        val hdrs = header.headers
        hdrs.get("Content-Type").isDefined must equalTo(true)
        hdrs.get("CONTENT-TYpe").isDefined must equalTo(true)
        hdrs.keys.find(header => header == "Content-Type" ).isDefined must equalTo(true)
        hdrs.keys.find(header => header == "CONTENT-TYpe" ).isDefined must equalTo(false)
        Iteratee.fold[Array[Byte],StringBuffer](new StringBuffer){ (buf,array) => { buf.append(array); buf }}
      }

      await(hp.map(_.run)).map(buf => buf.toString must contain("""{"Accept":"application/json"}""") )

      val content: String = await(WS.url("http://localhost:" + port + "/post").post("param1=foo")).body
      content must contain ("param1")
      content must contain("AnyContentAsText")
      content must contain ("foo")


      val contentForm: String = await(WS.url("http://localhost:" + port + "/post").post(Map("param1"->Seq("foo")))).body
      contentForm must contain ("AnyContentAsFormUrlEncoded")
      contentForm must contain ("foo")

       val jpromise: play.libs.F.Promise[play.libs.WS.Response] = play.libs.WS.url("http://localhost:" + port + "/post").setHeader("Content-Type","application/x-www-form-urlencoded").post("param1=foo")
      val contentJava: String = jpromise.get().getBody()
      contentJava must contain ("param1")
      contentJava must contain ("AnyContentAsFormUrlEncoded")
      contentJava must contain ("foo")

      browser.goTo("/form")
      browser.pageSource must contain("input type=\"radio\" id=\"gender_M\" name=\"gender\" value=\"M\" checked")

      browser.goTo("/")
      browser.pageSource must contain("Hello world")

      await(WS.url("http://localhost:" + port + "").get()).body must contain ("Hello world")

      await(WS.url("http://localhost:" + port + "/json").get()).json.as[User] must equalTo(User(1, "Sadek", List("tea")))

      browser.goTo("/conf")
      browser.pageSource must contain("This value comes from complex-app's complex1.conf")
      browser.pageSource must contain("override akka:2 second")
      browser.pageSource must contain("akka-loglevel:DEBUG")
      browser.pageSource must contain("promise-timeout:7000")
      browser.pageSource must contain("None")
      browser.title must beNull

      browser.goTo("/json_java")
      browser.pageSource must contain ("{\"peter\":\"foo\",\"yay\":\"value\"}")

      browser.goTo("/json_from_jsobject")
      browser.pageSource must contain ("{\"blah\":\"foo\"}")

      browser.goTo("/headers")
      browser.pageSource must contain("localhost:" + port)

      // --- Cookies

      browser.goTo("/json_java")
      browser.getCookies.size must equalTo(0)

      browser.goTo("/cookie")
      browser.getCookie("foo").getValue must equalTo("bar")

      browser.goTo("/read/foo")
      browser.pageSource must contain("Cookie foo has value: bar")

      browser.goTo("/read/bar")
      browser.pageSource must equalTo("")

      browser.goTo("/clear/foo")
      browser.getCookies.size must equalTo(0)

      // --- Javascript Reverse Router

      browser.goTo("/javascript-test?name=guillaume")

      browser.$("#route-url").click()
      browser.$("#result").getTexts().get(0) must equalTo ("/javascript-test?name=world")

      browser.$("#route-abs-url").click()
      browser.$("#result").getTexts().get(0) must equalTo ("http://localhost:" + port + "/javascript-test?name=world")

      browser.$("#route-abs-secure-url").click()
      browser.$("#result").getTexts().get(0) must equalTo ("https://localhost:" + port + "/javascript-test?name=world")

      browser.$("#route-abs-secure-url2").click()
      browser.$("#result").getTexts().get(0) must equalTo ("https://localhost:" + port + "/javascript-test?name=world")

      browser.$("#route-ws-url").click()
      browser.$("#result").getTexts().get(0) must equalTo ("ws://localhost:" + port + "/javascript-test?name=world")

      browser.$("#route-ws-secure-url").click()
      browser.$("#result").getTexts().get(0) must equalTo ("wss://localhost:" + port + "/javascript-test?name=world")

      browser.$("#route-ws-secure-url2").click()
      browser.$("#result").getTexts().get(0) must equalTo ("wss://localhost:" + port + "/javascript-test?name=world")
    }

    "Provide a hook to handle errors" in {
      "Synchronous results" in new WithBrowser() {
        browser.goTo("/sync-error")
        browser.pageSource must equalTo ("Something went wrong.")
      }
      "Asynchronous results" in new WithBrowser() {
        browser.goTo("/async-error")
        browser.pageSource must equalTo ("Something went wrong.")
      }
    }

    "% character in the query string" in new WithServer() {
      scala.io.Source.fromURL("http://localhost:" + port + "/?%") must throwAn[java.io.IOException].like {
        case e => e.getMessage must contain("Server returned HTTP response code: 400 for URL")
      }
    }

  }
  
}
