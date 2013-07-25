package scalaguide.http.scalasessionflash {

  import play.api.mvc._
  import play.api.test._
  import play.api.test.Helpers._
  import org.specs2.mutable.Specification
  import org.junit.runner.RunWith
  import org.specs2.runner.JUnitRunner
  import scala.concurrent.Future
  import org.specs2.execute.AsResult

  @RunWith(classOf[JUnitRunner])
  class ScalaSessionFlashSpec extends Specification with Controller {
    "A scala SessionFlash" should {

      "Reading a Session value" in {
        //#index-retrieve-incoming-session
        def index = Action { request =>
          request.session.get("connected").map { user =>
            Ok("Hello " + user)
          }.getOrElse {
            Unauthorized("Oops, you are not connected")
          }
        }
        //#index-retrieve-incoming-session

        assertAction(index, OK, FakeRequest().withSession("connected" -> "player"))(res => contentAsString(res) must contain("player"))
      }

      "Reading a Session value implicitly" in {
        //#index-retrieve-incoming-session-implicitly
        def index = Action { implicit request =>
          session.get("connected").map { user =>
            Ok("Hello " + user)
          }.getOrElse {
            Unauthorized("Oops, you are not connected")
          }
        }
        //#index-retrieve-incoming-session-implicitly
        assertAction(index, OK, FakeRequest().withSession("connected" -> "player"))(res => contentAsString(res) must contain("player"))
      }

      "Storing data in the Session" in {
        def storeSession = Action { implicit request =>
          //#store-session
          Ok("Welcome!").withSession(
            "connected" -> "user@gmail.com")
          //#store-session
        }

        assertAction(storeSession, OK, FakeRequest())(res => testSession(res, "connected", Some("user@gmail.com")))
      }

      "add data in the Session" in {
        def addSession = Action { implicit request =>
          //#add-session
          Ok("Hello World!").withSession(
            session + ("saidHello" -> "yes"))
          //#add-session
        }

        assertAction(addSession, OK, FakeRequest())(res => testSession(res, "saidHello", Some("yes")))
      }

      "remove data in the Session" in {
        def removeSession = Action { implicit request =>
          //#remove-session
          Ok("Theme reset!").withSession(
            session - "theme")
          //#remove-session
        }

        assertAction(removeSession, OK, FakeRequest().withSession("theme" -> "blue"))(res => testSession(res, "theme", None))
      }

      "Discarding the whole session" in {
        def discardingSession = Action { implicit request =>
          //#discarding-session
          Ok("Bye").withNewSession
          //#discarding-session
        }
        assertAction(discardingSession, OK, FakeRequest().withSession("theme" -> "blue"))(res => testSession(res, "theme", None))
      }

      "get from flash" in {
        //#using-flash
        def index = Action { implicit request =>
          Ok {
            flash.get("success").getOrElse("Welcome!")
          }
        }

        def save = Action {
          Redirect("/home").flashing(
            "success" -> "The item has been created")
        }
        //#using-flash
        assertAction(index, OK, FakeRequest().withFlash("success" -> "success!"))(res => contentAsString(res) must contain("success!"))
        assertAction(save, SEE_OTHER, FakeRequest())(res => testFlash(res, "success", Some("The item has been created")))
      }

      "fix could not find implicit value for parameter flash" in {
        //#find-noflash
        def index() = Action {
          implicit request =>
            Ok(views.html.Application.index())
        }
        //#find-noflash
        assertAction(index, OK, FakeRequest())(res => contentAsString(res) must contain("Good"))
      }

    }

    def testFlash(results: Future[SimpleResult], key: String, value: Option[String]) = {
      val flash = Helpers.flash(results)
      flash.get(key) === value
    }

    def testSession(results: Future[SimpleResult], key: String, value: Option[String]) = {
      val session = Helpers.session(results)
      session.get(key) === value
    }

    def assertAction[A, T: AsResult](action: Action[A], expectedResponse: Int = OK, request: => Request[A] = FakeRequest())(assertions: Future[SimpleResult] => T) = {
      val fakeApp = FakeApplication(additionalConfiguration = Map("application.secret" -> "pass"))
      running(fakeApp) {
        val result = action(request)
        status(result) must_== expectedResponse
        assertions(result)
      }
    }
  }

}

// Faking a form view
package views.html {
  object Application {
    def index() = "Good Job!"
  }
}



 