/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.http.scalasessionflash {
  import scala.concurrent.Future

  import org.junit.runner.RunWith
  import org.specs2.execute.AsResult
  import org.specs2.mutable.Specification
  import org.specs2.mutable.SpecificationLike
  import org.specs2.runner.JUnitRunner
  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._

  @RunWith(classOf[JUnitRunner])
  class ScalaSessionFlashSpec extends AbstractController(Helpers.stubControllerComponents()) with SpecificationLike {
    "A scala SessionFlash" should {
      "Reading a Session value" in {
        // #index-retrieve-incoming-session
        def index: Action[AnyContent] = Action { request =>
          request.session
            .get("connected")
            .map { user => Ok("Hello " + user) }
            .getOrElse {
              Unauthorized("Oops, you are not connected")
            }
        }
        // #index-retrieve-incoming-session

        assertAction(index, OK, FakeRequest().withSession("connected" -> "player"))(res =>
          contentAsString(res) must contain("player")
        )
      }

      "Storing data in the Session" in {
        def storeSession: Action[AnyContent] = Action { implicit request =>
          // #store-session
          Redirect("/home").withSession("connected" -> "user@gmail.com")
          // #store-session
        }

        assertAction(storeSession, SEE_OTHER, FakeRequest())(res =>
          testSession(res, "connected", Some("user@gmail.com"))
        )
      }

      "add data in the Session" in {
        def addSession(): Action[AnyContent] = Action { implicit request =>
          // #add-session
          Redirect("/home").withSession(request.session + ("saidHello" -> "yes"))
          // #add-session
        }

        assertAction(addSession(), SEE_OTHER, FakeRequest())(res => testSession(res, "saidHello", Some("yes")))
      }

      "remove data in the Session" in {
        def removeSession(): Action[AnyContent] = Action { implicit request =>
          // #remove-session
          Redirect("/home").withSession(request.session - "theme")
          // #remove-session
        }

        assertAction(removeSession(), SEE_OTHER, FakeRequest().withSession("theme" -> "blue"))(res =>
          testSession(res, "theme", None)
        )
      }

      "Discarding the whole session" in {
        def discardingSession: Action[AnyContent] = Action { implicit request =>
          // #discarding-session
          Redirect("/home").withNewSession
          // #discarding-session
        }
        assertAction(discardingSession, SEE_OTHER, FakeRequest().withSession("theme" -> "blue"))(res =>
          testSession(res, "theme", None)
        )
      }

      "get from flash" in {
        // #using-flash
        def index: Action[AnyContent] = Action { implicit request =>
          Ok {
            request.flash.get("success").getOrElse("Welcome!")
          }
        }

        def save: Action[AnyContent] = Action {
          Redirect("/home").flashing("success" -> "The item has been created")
        }
        // #using-flash
        assertAction(index, OK, FakeRequest().withFlash("success" -> "success!"))(res =>
          contentAsString(res) must contain("success!")
        )
        assertAction(save, SEE_OTHER, FakeRequest())(res =>
          testFlash(res, "success", Some("The item has been created"))
        )
      }

      "access flash in template" in {
        // #flash-implicit-request
        def index: Action[AnyContent] = Action { implicit request => Ok(views.html.index()) }
        // #flash-implicit-request

        assertAction(index, OK, FakeRequest())(result => contentAsString(result) must contain("Welcome!"))
        assertAction(index, OK, FakeRequest().withFlash("success" -> "Flashed!"))(result =>
          contentAsString(result) must contain("Flashed!")
        )
      }
    }

    def testFlash(results: Future[Result], key: String, value: Option[String]) = {
      val flash = Helpers.flash(results)
      flash.get(key) === value
    }

    def testSession(results: Future[Result], key: String, value: Option[String]) = {
      val session = Helpers.session(results)
      session.get(key) === value
    }

    def assertAction[A, T: AsResult](
        action: Action[A],
        expectedResponse: Int = OK,
        request: => Request[A] = FakeRequest()
    )(assertions: Future[Result] => T) = {
      running() { _ =>
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }
}
