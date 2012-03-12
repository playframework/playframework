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
   

    "reverse routes containing boolean parameters" in {
      "in the query string" in {
        controllers.routes.Application.takeBool(true).url must equalTo ("/take-bool?b=1")
        controllers.routes.Application.takeBool(false).url must equalTo ("/take-bool?b=0")
      }
      "in the  path" in {
        controllers.routes.Application.takeBool2(true).url must equalTo ("/take-bool-2/1")
        controllers.routes.Application.takeBool2(false).url must equalTo ("/take-bool-2/0")
      }
    }

    "bind boolean parameters" in {
      "from the query string" in {
        running(FakeApplication()) {
          val Some(result) = routeAndCall(FakeRequest(GET, controllers.routes.Application.takeBool(true).url))
          contentAsString(result) must equalTo ("true")
          val Some(result2) = routeAndCall(FakeRequest(GET, controllers.routes.Application.takeBool(false).url))
          contentAsString(result2) must equalTo ("false")
        }
      }
      "from the path" in {
        running(FakeApplication()) {
          val Some(result) = routeAndCall(FakeRequest(GET, controllers.routes.Application.takeBool2(true).url))
          contentAsString(result) must equalTo ("true")
          val Some(result2) = routeAndCall(FakeRequest(GET, controllers.routes.Application.takeBool2(false).url))
          contentAsString(result2) must equalTo ("false")
        }
      }
    }
  }
   
}
