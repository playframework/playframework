package test

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.ws._
import org.specs2.mutable._
import models._
import models.Protocol._
import org.openqa.selenium.htmlunit.HtmlUnitDriver


class FunctionalSpec extends Specification {
  "an Application" should {
    
    "call onClose for Ok.sendFile responses" in {
      import java.io.File
      running(TestServer(9003), HTMLUNIT) { browser =>
        def file = new File("onClose.tmp")
        file.createNewFile()
        file.exists() must equalTo(true)

        browser.goTo("http://localhost:9003/onCloseSendFile/" + file.getCanonicalPath)
        Thread.sleep(1000)
        file.exists() must equalTo(false)
      }
    }

    "pass functional test with two browsers" in {
      running(TestServer(9002), HTMLUNIT) { browser =>
        browser.goTo("http://localhost:9002")
        browser.pageSource must contain("Hello world")
      }
    } 
    "pass functional test" in {
      running(TestServer(9001), HTMLUNIT) { browser =>
        val content: String = await(WS.url("http://localhost:9001/post").post("param1=foo")).body
        content must contain ("param1")
        content must contain("AnyContentAsText")
        content must contain ("foo")


        val contentForm: String = await(WS.url("http://localhost:9001/post").post(Map("param1"->Seq("foo")))).body
        contentForm must contain ("AnyContentAsFormUrlEncoded")
        contentForm must contain ("foo")

         val jpromise: play.libs.F.Promise[play.libs.WS.Response] = play.libs.WS.url("http://localhost:9001/post").setHeader("Content-Type","application/x-www-form-urlencoded").post("param1=foo")
        val contentJava: String = jpromise.get().getBody()
        contentJava must contain ("param1")
        contentJava must contain ("AnyContentAsFormUrlEncoded")
        contentJava must contain ("foo")

        browser.goTo("http://localhost:9001/form")
        browser.pageSource must contain("input type=\"radio\" id=\"gender_M\" name=\"gender\" value=\"M\" checked")

        browser.goTo("http://localhost:9001")
        browser.pageSource must contain("Hello world")

        browser.goTo("http://localhost:9001/inherit")
        browser.pageSource must contain("I'm the parent action")

        await(WS.url("http://localhost:9001").get()).body must contain ("Hello world")

        await(WS.url("http://localhost:9001/json").get()).json.as[User] must equalTo(User(1, "Sadek", List("tea")))

        browser.goTo("http://localhost:9001/conf")
        browser.pageSource must contain("This value comes from complex-app's complex1.conf")
        browser.pageSource must contain("override akka:2 second")
        browser.pageSource must contain("akka-loglevel:DEBUG")
        browser.pageSource must contain("promise-timeout:7000")
        browser.pageSource must contain("None")
        browser.title must beNull

        browser.goTo("http://localhost:9001/json_java")
        browser.pageSource must contain ("{\"peter\":\"foo\",\"yay\":\"value\"}")

        browser.goTo("http://localhost:9001/json_from_jsobject")
        browser.pageSource must contain ("{\"blah\":\"foo\"}")

        browser.goTo("http://localhost:9001/headers")
        browser.pageSource must contain("localhost:9001")

        // --- Cookies

        browser.goTo("http://localhost:9001/json_java")
        browser.getCookies.size must equalTo(0)

        browser.goTo("http://localhost:9001/cookie")
        browser.getCookie("foo").getValue must equalTo("bar")

        browser.goTo("http://localhost:9001/read/foo")
        browser.pageSource must contain("Cookie foo has value: bar")

        browser.goTo("http://localhost:9001/read/bar")
        browser.pageSource must equalTo("")

        browser.goTo("http://localhost:9001/clear/foo")
        browser.getCookies.size must equalTo(0)

        // --- Javascript Reverse Router

        browser.goTo("http://localhost:9001/javascript-test?name=guillaume")

        browser.$("#route-url").click()
        browser.$("#result").getTexts().get(0) must equalTo ("/javascript-test?name=world")

        browser.$("#route-abs-url").click()
        browser.$("#result").getTexts().get(0) must equalTo ("http://localhost:9001/javascript-test?name=world")

        browser.$("#route-abs-secure-url").click()
        browser.$("#result").getTexts().get(0) must equalTo ("https://localhost:9001/javascript-test?name=world")

        browser.$("#route-abs-secure-url2").click()
        browser.$("#result").getTexts().get(0) must equalTo ("https://localhost:9001/javascript-test?name=world")

        browser.$("#route-ws-url").click()
        browser.$("#result").getTexts().get(0) must equalTo ("ws://localhost:9001/javascript-test?name=world")

        browser.$("#route-ws-secure-url").click()
        browser.$("#result").getTexts().get(0) must equalTo ("wss://localhost:9001/javascript-test?name=world")

        browser.$("#route-ws-secure-url2").click()
        browser.$("#result").getTexts().get(0) must equalTo ("wss://localhost:9001/javascript-test?name=world")
      }
    }

  }
  
}
