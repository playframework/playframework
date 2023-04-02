/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.ws.scalaopenid

// format: off
import play.api.test._
// format: on

//#dependency
import javax.inject.Inject

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid._
import play.api.mvc._

class IdController @Inject() (val openIdClient: OpenIdClient, c: ControllerComponents)(
    implicit val ec: ExecutionContext
) extends AbstractController(c)
//#dependency

class ScalaOpenIdSpec extends PlaySpecification {
  "Scala OpenId" should {
    "be injectable" in new WithApplication() with Injecting {
      override def running() = {
        val controller =
          new IdController(inject[OpenIdClient], inject[ControllerComponents])(inject[ExecutionContext]) with Logging {
            // #flow
            def login = Action {
              Ok(views.html.login())
            }

            def loginPost = Action.async { implicit request =>
              Form(
                single(
                  "openid" -> nonEmptyText
                )
              ).bindFromRequest()
                .fold(
                  { error =>
                    logger.info(s"bad request ${error.toString}")
                    Future.successful(BadRequest(error.toString))
                  },
                  { openId =>
                    openIdClient
                      .redirectURL(openId, routes.Application.openIdCallback.absoluteURL())
                      .map(url => Redirect(url))
                      .recover { case t: Throwable => Redirect(routes.Application.login) }
                  }
                )
            }

            def openIdCallback = Action.async { implicit request: Request[AnyContent] =>
              openIdClient
                .verifiedId(request)
                .map(info => Ok(info.id + "\n" + info.attributes))
                .recover {
                  case t: Throwable =>
                    // Here you should look at the error, and give feedback to the user
                    Redirect(routes.Application.login)
                }
            }
            // #flow

            def extended(openId: String)(implicit request: RequestHeader) = {
              // #extended
              openIdClient.redirectURL(
                openId,
                routes.Application.openIdCallback.absoluteURL(),
                Seq("email" -> "http://schema.openid.net/contact/email")
              )
              // #extended
            }
          }
        controller must beAnInstanceOf[IdController]
      }
    }
  }
}

object routes {
  object Application {
    val login          = Call("GET", "login")
    val openIdCallback = Call("GET", "callback")
  }
}

package views {
  object html {
    def login() = "loginpage"
  }
}
