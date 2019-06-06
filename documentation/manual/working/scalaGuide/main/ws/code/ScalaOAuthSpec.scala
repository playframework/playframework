/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package scalaguide.ws.scalaoauth

import play.api.test._

import scala.concurrent.ExecutionContext

//#dependency
import javax.inject.Inject
import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.oauth._
import play.api.libs.ws._

class HomeController @Inject()(val wsClient: WSClient, c: ControllerComponents)(implicit val ec: ExecutionContext)
    extends AbstractController(c)
//#dependency

object routes {
  object Application {
    val authenticate = Call("GET", "authenticate")
    val index        = Call("GET", "index")
  }
}

class ScalaOAuthSpec extends PlaySpecification {

  "Scala OAuth" should {
    "be injectable" in new WithApplication() with Injecting {
      val controller = new HomeController(inject[WSClient], inject[ControllerComponents])(inject[ExecutionContext]) {
        //#flow
        val KEY = ConsumerKey("xxxxx", "xxxxx")

        val oauth = OAuth(
          ServiceInfo(
            "https://api.twitter.com/oauth/request_token",
            "https://api.twitter.com/oauth/access_token",
            "https://api.twitter.com/oauth/authorize",
            KEY
          ),
          true
        )

        def sessionTokenPair(implicit request: RequestHeader): Option[RequestToken] = {
          for {
            token  <- request.session.get("token")
            secret <- request.session.get("secret")
          } yield {
            RequestToken(token, secret)
          }
        }

        def authenticate = Action { request: Request[AnyContent] =>
          request
            .getQueryString("oauth_verifier")
            .map { verifier =>
              val tokenPair = sessionTokenPair(request).get
              // We got the verifier; now get the access token, store it and back to index
              oauth.retrieveAccessToken(tokenPair, verifier) match {
                case Right(t) => {
                  // We received the authorized tokens in the OAuth object - store it before we proceed
                  Redirect(routes.Application.index).withSession("token" -> t.token, "secret" -> t.secret)
                }
                case Left(e) => throw e
              }
            }
            .getOrElse(oauth.retrieveRequestToken("https://localhost:9000/auth") match {
              case Right(t) => {
                // We received the unauthorized tokens in the OAuth object - store it before we proceed
                Redirect(oauth.redirectUrl(t.token)).withSession("token" -> t.token, "secret" -> t.secret)
              }
              case Left(e) => throw e
            })
        }
        //#flow

        //#extended
        def timeline = Action.async { implicit request: Request[AnyContent] =>
          sessionTokenPair match {
            case Some(credentials) => {
              wsClient
                .url("https://api.twitter.com/1.1/statuses/home_timeline.json")
                .sign(OAuthCalculator(KEY, credentials))
                .get
                .map(result => Ok(result.json))
            }
            case _ => Future.successful(Redirect(routes.Application.authenticate))
          }
        }
        //#extended
      }
      controller must beAnInstanceOf[HomeController]
    }
  }

}
