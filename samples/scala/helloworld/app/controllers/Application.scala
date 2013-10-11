package controllers

import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.libs.functional.syntax._

import views._

object Application extends Controller {

  /**
   * Describes the hello form.
   */
  implicit val helloValidation = From[UrlFormEncoded] { __ =>
    import Rules._
    ((__ \ "name").read(notEmpty) ~
     (__ \ "repeat").read(min(1) |+| max(100)) ~
     (__ \ "color").read[Option[String]]).tupled
  }

  implicit val computerW = To[UrlFormEncoded] { __ =>
    import Writes._
    ((__ \ "name").write[String] ~
     (__ \ "repeat").write[Int] ~
     (__ \ "color").write[Option[String]]).tupled
  }

  // -- Actions

  /**
   * Home page
   */
  def index = Action {
    Ok(html.index(Form()))
  }

  /**
   * Handles the form submission.
   */
  def sayHello = Action { implicit request =>
    val data = request.queryString
    val r = helloValidation.validate(data)
    r match {
      case Failure(_) => BadRequest(html.index(Form(data, r)))
      case Success((name, repeat, color)) => Ok(html.hello(name, repeat.toInt, color))
    }
  }

}
