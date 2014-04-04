/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.ws.scalaws

//#ScalaOpenID
package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.openid.{ Errors, OpenID, OpenIDError }

object LoginController extends Controller {

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  def index = Action {
    Ok("HTML LOGIN FORM HERE")
  }

  def indexPost = Action.async { implicit request =>
    Form(single(
      "openid" -> nonEmptyText
    )).bindFromRequest.fold(
      error => {
        Logger.info("bad request " + error.toString)
        Future.successful { BadRequest(error.toString) }
      },
      {
        case (openid) => {
          OpenID.redirectURL(openid,
                             "http://localhost:9000/login/callback", //routes.LoginController.callback.absoluteURL(),
                             Seq("fullname" -> "http://axschema.org/namePerson",
                                 "email" -> "http://axschema.org/contact/email",
                                 "image" -> "http://axschema.org/media/image/default"))
          .map( url => Redirect(url) )
          .recover {
            case e: OpenIDError => {
              Redirect("/login").flashing(
                "error" -> e.message
              )
            }
          }
        }
      }
    )
  }

  def callback = Action.async { implicit request =>
    OpenID.verifiedId.map(info => {

      info.attributes.get("email") match {
        case None => {
          Redirect("/login").flashing(
            "error" -> "Open ID account did not provide your email address. Please try again or use a different OpenID"
          )
        }

        case Some(email: String) => {
          val fullname = info.attributes.get("fullname")
          val imageUrl = info.attributes.get("image_url")
          // Example: val user = User.upsert(email, fullname, imageUrl)
          Redirect("/").withSession { "user_email" -> email.toString }
        }
      }
    })

    .recover {
      case e: Throwable => {
        Logger.error("Error authenticating open id user", e)
        Redirect("/login").flashing(
          "error" -> "Error authenticating. Please try again or provide a different OpenID URL"
        )
      }
    }

  }
}


//#ScalaOpenID
