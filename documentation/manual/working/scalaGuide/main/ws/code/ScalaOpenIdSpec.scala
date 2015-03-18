package scalaguide.ws.scalaopenid

import play.api.test._

//#dependency
import javax.inject.Inject
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid._

class Application @Inject() (openIdClient: OpenIdClient) extends Controller {

}
//#dependency


object ScalaOpenIdSpec extends PlaySpecification {

  "Scala OpenId" should {
    "be injectable" in new WithApplication() {
      app.injector.instanceOf[Application] must beAnInstanceOf[Application]
    }
  }

  def openIdClient: OpenIdClient = null

  import play.api.mvc.Results._

  //#flow
  def login = Action {
    Ok(views.html.login())
  }

  def loginPost = Action.async { implicit request =>
    Form(single(
      "openid" -> nonEmptyText
    )).bindFromRequest.fold({ error =>
      Logger.info("bad request " + error.toString)
      Future.successful(BadRequest(error.toString))
    }, { openId =>
      openIdClient.redirectURL(openId, routes.Application.openIdCallback.absoluteURL())
        .map(url => Redirect(url))
        .recover { case t: Throwable => Redirect(routes.Application.login)}
    })
  }

  def openIdCallback = Action.async { implicit request =>
    openIdClient.verifiedId(request).map(info => Ok(info.id + "\n" + info.attributes))
      .recover {
      case t: Throwable =>
        // Here you should look at the error, and give feedback to the user
        Redirect(routes.Application.login)
    }
  }
  //#flow

  def extended(openId: String)(implicit request: RequestHeader) = {
    //#extended
    openIdClient.redirectURL(
      openId,
      routes.Application.openIdCallback.absoluteURL(),
      Seq("email" -> "http://schema.openid.net/contact/email")
    )
    //#extended
  }
}

object routes {
  object Application {
    val login = Call("GET", "login")
    val openIdCallback = Call("GET", "callback")
  }
}

package views {
object html {
  def login() = "loginpage"
}
}