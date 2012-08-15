package test

import play.api.test._
import play.api.test.Helpers._

import org.specs2.mutable._
import models._
import play.api.mvc.AnyContentAsEmpty

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

    "tess custom validator failure" in {
      import play.data._
      running(FakeApplication()) {
         val userForm = new Form(classOf[JUser])
         val anyData = new java.util.HashMap[String,String]
         anyData.put("email", "")
         userForm.bind(anyData).errors.toString must contain("ValidationError(email,error.invalid,[class validator.NotEmpty]")
      }
    }
    "tess custom validator passing" in {
      import play.data._
      running(FakeApplication()) {
         val userForm = new Form(classOf[JUser])
         val anyData = new java.util.HashMap[String,String]
         anyData.put("email", "peter.hausel@yay.com")
         userForm.bind(anyData).get.toString must contain ("")
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

    def javaResult(result: play.api.mvc.Result) =
      new play.mvc.Result {
        def getWrappedResult = result
      }

    "execute json with content type" in {
     running(FakeApplication()) {
       // here we just test the case insensitivity of FakeHeaders, which is not that
       // interesting, ...
         val Some(result) = routeAndCall(FakeRequest(GET, "/jsonWithContentType",
           FakeHeaders(Map("Accept"-> Seq("application/json"))), AnyContentAsEmpty))
         status(result) must equalTo(OK)
         contentType(result) must equalTo(Some("application/json"))
         charset(result) must equalTo(None)
         contentAsString(result) must contain("""{"Accept":"application/json"}""")
         play.test.Helpers.charset(javaResult(result)) must equalTo(null)
       }
     }


    "execute json with content type and charset" in {
     running(FakeApplication()) {
         val Some(result) = routeAndCall(FakeRequest(GET, "/jsonWithCharset"))
         status(result) must equalTo(OK)
         contentType(result) must equalTo(Some("application/json"))
         charset(result) must equalTo(Some("utf-8"))
         play.test.Helpers.charset(javaResult(result)) must equalTo("utf-8")
       }
     }

    "not serve asset directories" in {
      running(FakeApplication()) {
        val Some(result) = routeAndCall(FakeRequest(GET, "/public//"))
        status(result) must equalTo (NOT_FOUND)
      }
    }
   
    "remove cache elements" in {
      running(FakeApplication()) {
        import play.api.Play.current
        import play.api.cache.Cache
        Cache.set("foo", "bar")
        Cache.get("foo") must equalTo (Some("bar"))
        Cache.remove("foo")
        Cache.get("foo") must equalTo (None)
    }}

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

    "bind int parameters from the query string as a list" in {
      running(FakeApplication()) {
        "from a list of numbers" in {
          val Some(result) = routeAndCall(FakeRequest(GET, controllers.routes.Application.takeList(List(1, 2, 3)).url))
          contentAsString(result) must equalTo ("123")
        }
        "from a list of numbers and letters" in {
          val Some(result) = routeAndCall(FakeRequest(GET, "/take-list?x=1&x=a&x=2"))
          contentAsString(result) must equalTo ("12")
        }
        "when there is no parameter at all" in {
          val Some(result) = routeAndCall(FakeRequest(GET, "/take-list"))
          contentAsString(result) must equalTo ("")
        }
        "using the Java API" in {
          val Some(result) = routeAndCall(FakeRequest(GET, "/take-list-java?x=1&x=2&x=3"))
          contentAsString(result) must equalTo ("3 elements")
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

  }

}
