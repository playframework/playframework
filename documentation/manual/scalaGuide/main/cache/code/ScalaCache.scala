
/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.cache {

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import play.api.Play.current
import play.api.test._
import play.api.mvc._
import play.api.libs.json.Json
import scala.concurrent.Future
import org.specs2.execute.AsResult


@RunWith(classOf[JUnitRunner])
class ScalaCacheSpec extends PlaySpecification with Controller {

  import play.api.cache.CacheApi
  import play.api.cache.Cached

  def withCache[T](block: CacheApi => T) = {
    val app = FakeApplication()
    running(app)(block(app.injector.instanceOf[CacheApi]))
  }

  "A scala Cache" should {

    "be injectable" in {
      val app = FakeApplication()
      running(app) {
        app.injector.instanceOf[inject.Application]
        ok
      }
    }

    "a cache" in withCache { cache =>
      val connectedUser = User("xf")
      //#set-value
      cache.set("item.key", connectedUser)
      //#set-value

      //#get-value
      val maybeUser: Option[User] = cache.get[User]("item.key")
      //#get-value

      maybeUser must beSome(connectedUser)

      //#remove-value
      cache.remove("item.key")
      //#remove-value
      cache.get[User]("item.key") must beNone
    }


    "a cache or get user" in withCache { cache =>
      val connectedUser = "xf"
      //#retrieve-missing
      val user: User = cache.getOrElse[User]("item.key") {
        User.findById(connectedUser)
      }
      //#retrieve-missing
      user must beEqualTo(User(connectedUser))
    }

    "cache with expiry" in withCache { cache =>
      val connectedUser = "xf"
      //#set-value-expiration
      import scala.concurrent.duration._

      cache.set("item.key", connectedUser, 5.minutes)
      //#set-value-expiration
      ok
    }

    "bind multiple" in {
      val app = FakeApplication(additionalConfiguration = Map("play.modules.cache.bindCaches" -> Seq("session-cache")))
      running(app) {
        app.injector.instanceOf[qualified.Application]
        ok
      }
    }

    "cached page" in {
      running(FakeApplication()) {
        //#cached-action
        def index = Cached("homePage") {
          Action {
            Ok("Hello world")
          }
        }
        //#cached-action
        val result = index(FakeRequest()).run
        status(result) must_== 200
      }
    }

    "composition cached page" in {
      import play.api.mvc.Security.Authenticated

      //#composition-cached-action
      def userProfile = Authenticated {
        user =>
          Cached(req => "profile." + user) {
            Action {
              Ok(views.html.profile(User.find(user)))
            }
          }
      }
      //#composition-cached-action
      testAction(action=userProfile,expectedResponse=UNAUTHORIZED)
    }

    "control cache" in {
      running(FakeApplication()) {
        //#cached-action-control
        def get(index: Int) = Cached.status(_ => "/resource/"+ index, 200) {
          Action {
            if (index > 0) {
              Ok(Json.obj("id" -> index))
            } else {
              NotFound
            }
          }
        }
        //#cached-action-control
        val result0 = get(1)(FakeRequest("GET", "/resource/1")).run
        status(result0) must_== 200
        val result1 = get(-1)(FakeRequest("GET", "/resource/-1")).run
        status(result1) must_== 404
      }

    }

    "control cache" in {
      running(FakeApplication()) {
        //#cached-action-control-404
        def get(index: Int) = {
          val caching = Cached
            .status(_ => "/resource/"+ index, 200)
            .includeStatus(404, 600)

          caching {
            Action {
              if (index % 2 == 1) {
                Ok(Json.obj("id" -> index))
              } else {
                NotFound
              }
            }
          }
        }
        //#cached-action-control-404
        val result0 = get(1)(FakeRequest("GET", "/resource/1")).run
        status(result0) must_== 200
        val result1 = get(2)(FakeRequest("GET", "/resource/2")).run
        status(result1) must_== 404
      }

    }


  }


  def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
    assertAction(action, request, expectedResponse) {
      result => success
    }
  }

  def assertAction[A, T: AsResult](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[Result] => T) = {
    running(FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))) {
      val result = action(request).run
      status(result) must_== expectedResponse
      assertions(result)
    }
  }


}

package views {

object html {
  def profile(user: User) = {
    s"Hello, $user.name"
  }
}

}

package inject {
//#inject
import play.api.cache._
import play.api.mvc._
import javax.inject.Inject

class Application @Inject() (cache: CacheApi) extends Controller {

}
//#inject
}

package qualified {
//#qualified
import play.api.cache._
import play.api.mvc._
import javax.inject.Inject

class Application @Inject()(
    @NamedCache("session-cache") sessionCache: CacheApi
) extends Controller {

}
//#qualified
}


case class User(name: String)

object User {
  def findById(userId: String) = User(userId)

  def find(user: String) = User(user)
}

}

