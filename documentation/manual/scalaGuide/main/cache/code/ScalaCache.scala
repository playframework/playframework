
package scalaguide.cache {

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import play.api.Play.current
import play.api.test._
import play.api.cache.{Cached, Cache}
import play.api.mvc._
import scala.concurrent.Future
import org.specs2.execute.AsResult


@RunWith(classOf[JUnitRunner])
class ScalaCacheSpec extends PlaySpecification with Controller {

  "A scala Cache" should {

    "a cache" in {
      running(FakeApplication()) {
        val connectedUser = User("xf")
        //#set-value
        Cache.set("item.key", connectedUser)
        //#set-value

        //#get-value
        val maybeUser: Option[User] = Cache.getAs[User]("item.key")
        //#get-value

        maybeUser must beSome(connectedUser)

        //#remove-value
        Cache.remove("item.key")
        //#remove-value
        Cache.getAs[User]("item.key") must beNone

      }
    }


    "a cache or get user" in {
      running(FakeApplication()) {
        val connectedUser = "xf"
        //#retrieve-missing
        val user: User = Cache.getOrElse[User]("item.key") {
          User.findById(connectedUser)
        }
        //#retrieve-missing
        user must beEqualTo(User(connectedUser))
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


  }


  def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
    assertAction(action, request, expectedResponse) {
      result => success
    }
  }

  def assertAction[A, T: AsResult](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK)(assertions: Future[SimpleResult] => T) = {
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


case class User(name: String)

object User {
  def findById(userId: String) = User(userId)

  def find(user: String) = User(user)
}

}

