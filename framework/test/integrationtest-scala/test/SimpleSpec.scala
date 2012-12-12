package test

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json.{JsValue, Json, JsObject}
import scala.concurrent.duration.Duration
import scala.concurrent.Await

class SimpleSpec extends Specification {
  
  "my application" should {
    
    "compute 1 + 1" in {
      val a = 1 + 1
      
      a must equalTo(2)
    }
    
    "render index template" in {
      val html = views.html.index("Coco")
      
      contentType(html) must equalTo("text/html")
      contentAsString(html) must contain("Hello Coco")
    }
    
    "respond to the index Action" in {
      val result = controllers.Application.index("Kiki")(FakeRequest())
      
      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("text/html"))
      charset(result) must equalTo(Some("utf-8"))
      contentAsString(result) must contain("Hello Kiki")
    }
    
    "do not respond to a wrong url" in {
      running(FakeApplication()) {
        val result = route(FakeRequest(POST, "/"))
        
        result must equalTo(None)
      }
    }
    
    "respond to the GET /Kiki request" in {
      running(FakeApplication()) {
        val Some(result) = route(FakeRequest(GET, "/Kiki"))
        
        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("text/html"))
        charset(result) must equalTo(Some("utf-8"))
        contentAsString(result) must contain("Hello Kiki")
      }
    }
    
    "respond to the key Action" in {
      running(FakeApplication()) {
        val result = controllers.Application.key(FakeRequest())

        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("text/plain"))
        charset(result) must equalTo(Some("utf-8"))
        contentAsString(result) must contain("secret")
      }
    }
    
    "run in a browser" in {
      running(TestServer(3333), HTMLUNIT) { browser =>
        
        browser.goTo("http://localhost:3333")
        browser.$("#title").getTexts().get(0) must equalTo("Hello Guest")
        
        browser.$("a").click()
        
        browser.url must equalTo("http://localhost:3333/Coco")
        browser.$("#title").getTexts().get(0) must equalTo("Hello Coco")

      }
    }

    "response to the json Action using default (POST) method" in {
      running(FakeApplication()) {
        val node = Json.toJson(Map("key1" -> "val1", "key2" -> "2", "key3" -> "true"))
        val result = controllers.Application.json(FakeRequest().withJsonBody(node))

        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("application/json"))
        val node2 = Json.parse(contentAsString(result))
        (node2 \ "key1").as[String] must equalTo("val1")
        (node2 \ "key2").as[String] must equalTo("2")
        (node2 \ "key3").as[String] must equalTo("true")
      }
    }

    "response to the json Action when specifying the method (DELETE)" in {
      running(FakeApplication()) {
        val node = Json.toJson(Map("key1" -> "val1", "key2" -> "2", "key3" -> "true"))
        val result = controllers.Application.json(FakeRequest().withJsonBody(node, "DELETE"))

        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("application/json"))
        val node2 = Json.parse(contentAsString(result))
        (node2 \ "key1").as[String] must equalTo("val1")
        (node2 \ "key2").as[String] must equalTo("2")
        (node2 \ "key3").as[String] must equalTo("true")
      }
    }

    "execute in the user execution context" in new WithServer() {
      val response = Await.result(wsCall(controllers.routes.Application.thread()).get(), Duration.Inf)
      response.body must startWith("play-akka.actor.default-dispatcher-")
    }

    "execute body parser in the user execution context" in new WithServer() {
      val response = Await.result(wsCall(controllers.routes.Application.bodyParserThread()).get(), Duration.Inf)
      response.body must startWith("play-akka.actor.default-dispatcher-")
    }

  }

}
