/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.cache {

  import akka.Done
  import akka.stream.ActorMaterializer
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import org.specs2.execute.AsResult

  import play.api.Play.current
  import play.api.test._
  import play.api.mvc._
  import play.api.libs.json.Json
  import scala.concurrent.Await
  import scala.concurrent.Future
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext

  @RunWith(classOf[JUnitRunner])
  class ScalaCacheSpec extends AbstractController(Helpers.stubControllerComponents()) with PlaySpecification {

    import play.api.cache.AsyncCacheApi
    import play.api.cache.Cached

    def withCache[T](block: AsyncCacheApi => T) = {
      running()(app => block(app.injector.instanceOf[AsyncCacheApi]))
    }

    "A scala Cache" should {

      "be injectable" in {
        running() { app =>
          app.injector.instanceOf[inject.Application]
          ok
        }
      }

      "a cache" in withCache { cache =>
        val connectedUser = User("xf")
        //#set-value
        val result: Future[Done] = cache.set("item.key", connectedUser)
        //#set-value
        Await.result(result, 1.second)

        //#get-value
        val futureMaybeUser: Future[Option[User]] = cache.get[User]("item.key")
        //#get-value

        val maybeUser = Await.result(futureMaybeUser, 1.second)
        maybeUser must beSome(connectedUser)

        //#remove-value
        val removeResult: Future[Done] = cache.remove("item.key")
        //#remove-value

        //#removeAll-values
        val removeAllResult: Future[Done] = cache.removeAll()
        //#removeAll-values

        Await.result(removeResult, 1.second)

        cache.sync.get[User]("item.key") must beNone
      }

      "a cache or get user" in withCache { cache =>
        val connectedUser = "xf"
        //#retrieve-missing
        val futureUser: Future[User] = cache.getOrElseUpdate[User]("item.key") {
          User.findById(connectedUser)
        }
        //#retrieve-missing
        val user = Await.result(futureUser, 1.second)
        user must beEqualTo(User(connectedUser))
      }

      "cache with expiry" in withCache { cache =>
        val connectedUser = "xf"
        //#set-value-expiration
        import scala.concurrent.duration._

        val result: Future[Done] = cache.set("item.key", connectedUser, 5.minutes)
        //#set-value-expiration
        Await.result(result, 1.second)
        ok
      }

      "bind multiple" in {
        running(_.configure("play.cache.bindCaches" -> Seq("session-cache"))) { app =>
          app.injector.instanceOf[qualified.Application]
          ok
        }
      }

      "cached page" in {
        running() { app =>
          implicit val mat = ActorMaterializer()(app.actorSystem)
          val cachedApp    = app.injector.instanceOf[cachedaction.Application1]
          val result       = cachedApp.index(FakeRequest()).run()
          status(result) must_== 200
        }
      }

      "composition cached page" in {
        running() { app =>
          val cachedApp = app.injector.instanceOf[cachedaction.Application1]
          testAction(action = cachedApp.userProfile, expectedResponse = UNAUTHORIZED)
        }
      }

      "control cache" in {
        running() { app =>
          implicit val mat = ActorMaterializer()(app.actorSystem)
          val cachedApp    = app.injector.instanceOf[cachedaction.Application1]
          val result0      = cachedApp.get(1)(FakeRequest("GET", "/resource/1")).run()
          status(result0) must_== 200
          val result1 = cachedApp.get(-1)(FakeRequest("GET", "/resource/-1")).run()
          status(result1) must_== 404
        }

      }

      "control cache" in {
        running() { app =>
          implicit val mat = ActorMaterializer()(app.actorSystem)
          val cachedApp    = app.injector.instanceOf[cachedaction.Application2]
          val result0      = cachedApp.get(1)(FakeRequest("GET", "/resource/1")).run()
          status(result0) must_== 200
          val result1 = cachedApp.get(2)(FakeRequest("GET", "/resource/2")).run()
          status(result1) must_== 404
        }
      }

    }

    def testAction[A](action: EssentialAction, request: => Request[A] = FakeRequest(), expectedResponse: Int = OK) = {
      assertAction(action, request, expectedResponse) { result =>
        success
      }
    }

    def assertAction[A, T: AsResult](
        action: EssentialAction,
        request: => Request[A] = FakeRequest(),
        expectedResponse: Int = OK
    )(assertions: Future[Result] => T) = {
      running() { app =>
        implicit val mat = ActorMaterializer()(app.actorSystem)
        val result       = action(request).run()
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

    class Application @Inject()(cache: AsyncCacheApi, cc: ControllerComponents) extends AbstractController(cc) {}
//#inject
  }

  package qualified {
//#qualified
    import play.api.cache._
    import play.api.mvc._
    import javax.inject.Inject

    class Application @Inject()(
        @NamedCache("session-cache") sessionCache: AsyncCacheApi,
        cc: ControllerComponents
    ) extends AbstractController(cc) {}
//#qualified
  }

  package cachedaction {
//#cached-action-app
    import play.api.cache.Cached
    import javax.inject.Inject

    class Application @Inject()(cached: Cached, cc: ControllerComponents) extends AbstractController(cc) {}
//#cached-action-app

    class Application1 @Inject()(cached: Cached, cc: ControllerComponents)(implicit ec: ExecutionContext)
        extends AbstractController(cc) {
      //#cached-action
      def index = cached("homePage") {
        Action {
          Ok("Hello world")
        }
      }
      //#cached-action

      import play.api.mvc.Security._

      //#composition-cached-action
      def userProfile = WithAuthentication(_.session.get("username")) { userId =>
        cached(req => "profile." + userId) {
          Action.async {
            User.find(userId).map { user =>
              Ok(views.html.profile(user))
            }
          }
        }
      }
      //#composition-cached-action
      //#cached-action-control
      def get(index: Int) = cached.status(_ => "/resource/" + index, 200) {
        Action {
          if (index > 0) {
            Ok(Json.obj("id" -> index))
          } else {
            NotFound
          }
        }
      }
      //#cached-action-control
    }
    class Application2 @Inject()(cached: Cached, cc: ControllerComponents) extends AbstractController(cc) {
      //#cached-action-control-404
      def get(index: Int) = {
        val caching = cached
          .status(_ => "/resource/" + index, 200)
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
    }

  }

  case class User(name: String)

  object User {
    def findById(userId: String) = Future.successful(User(userId))

    def find(user: String) = Future.successful(User(user))
  }

}
