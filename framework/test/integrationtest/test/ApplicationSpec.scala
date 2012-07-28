package test

import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._

class ApplicationSpec extends Specification {

  "an Application" should {
  
    "execute index" in {
      
      running(FakeApplication()) {
        
        val action = controllers.Application.index()
        val result = action(FakeRequest())
        
        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("text/html"))
        charset(result) must equalTo(Some("utf-8"))
        contentAsString(result) must contain("Hello world")
      }
    }
  
    "execute index again" in {
      
      running(FakeApplication()) {
        val action = controllers.Application.index()
        val result = action(FakeRequest())
        
        status(result) must equalTo(OK)
        contentType(result) must equalTo(Some("text/html"))
        charset(result) must equalTo(Some("utf-8"))
        contentAsString(result) must contain("Hello world")
      }
    }
    
    "execute json" in {
    running(FakeApplication()) {
      val Some(result) = routeAndCall(FakeRequest(GET, "/json"))
      status(result) must equalTo(OK)
      contentType(result) must equalTo(Some("application/json"))
      contentAsString(result) must contain("{\"id\":1,\"name\":\"Sadek\",\"favThings\":[\"tea\"]}")

      }
    }
    
    "not serve asset directories" in {
      running(FakeApplication()) {
        val Some(result) = routeAndCall(FakeRequest(GET, "/public//"))
        status(result) must equalTo (NOT_FOUND)
      }
    }
   
    "urldecode correctly parameters from path and query string" in {
      running(FakeApplication()) {
        val Some(result) = routeAndCall(FakeRequest(GET, "/urldecode/2%2B2?q=2%2B2"))
        contentAsString(result) must contain ("fromPath=2+2")
        contentAsString(result) must contain ("fromQueryString=2+2")
      }
    }

    "test Accept header mime-types" in {
      import play.api.http.HeaderNames._
      "Scala API" in {
        running(FakeApplication()) {
          val url = controllers.routes.Application.accept().url
          val Some(result) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html,application/xml;q=0.5"))
          contentAsString(result) must equalTo ("html")

          val Some(result2) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/*"))
          contentAsString(result2) must equalTo ("html")

          val Some(result3) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "application/json"))
          contentAsString(result3) must equalTo ("json")
        }
      }
      "Java API" in {
        running(FakeApplication()) {
          val url = controllers.routes.JavaApi.accept().url
          val Some(result) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/html,application/xml;q=0.5"))
          contentAsString(result) must equalTo ("html")

          val Some(result2) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "text/*"))
          contentAsString(result2) must equalTo ("html")

          val Some(result3) = routeAndCall(FakeRequest(GET, url).withHeaders(ACCEPT -> "application/json"))
          contentAsString(result3) must equalTo ("json")
        }
      }
    }

    "return jsonp" in {
      "Scala API" in {
        running(FakeApplication()) {
          val Some(result) = routeAndCall(FakeRequest(GET, controllers.routes.Application.jsonp("baz").url))
          contentAsString(result) must equalTo ("baz({\"foo\":\"bar\"});")
          contentType(result) must equalTo (Some("text/javascript"))
        }
      }
      "Java API" in {
        running(FakeApplication()) {
          val Some(result) = routeAndCall(FakeRequest(GET, controllers.routes.JavaApi.jsonpJava("baz").url))
          contentAsString(result) must equalTo ("baz({\"foo\":\"bar\"});")
          contentType(result) must equalTo (Some("text/javascript"))
        }
      }
    }

  }

}
