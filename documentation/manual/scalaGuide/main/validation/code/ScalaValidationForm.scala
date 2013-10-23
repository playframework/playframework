/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.validation

import org.specs2.mutable.Specification

object ScalaValidationFormSpec extends Specification with play.api.mvc.Controller {

  import java.util.Date
  import play.api.mvc._
  import play.api.data.mapping._
  import play.api.libs.functional.syntax._

  case class Company(id: Option[Long] = None, name: String)
  case class Computer(id: Option[Long] = None, name: String, introduced: Option[Date], discontinued: Option[Date], companyId: Option[Long])

  //#form-computer-write
  implicit val computerW = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.Writes._
    ((__ \ "id").write[Option[Long]] ~
     (__ \ "name").write[String] ~
     (__ \ "introduced").write(option(date("yyyy-MM-dd"))) ~
     (__ \ "discontinued").write(option(date("yyyy-MM-dd"))) ~
     (__ \ "company").write[Option[Long]]) (unlift(Computer.unapply _))
  }
  //#form-computer-write

  //#form-computer-rule
  implicit val computerValidation = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.Rules._
    ((__ \ "id").read(ignored[UrlFormEncoded, Option[Long]](None)) ~
     (__ \ "name").read(notEmpty) ~
     (__ \ "introduced").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "discontinued").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "company").read[Option[Long]]) (Computer.apply _)
  }
  //#form-computer-rule

  // fake objects
  val Home = Redirect("/foo")

  object Computer {
    def edit = ???
    def findById(id: Long): Option[Computer] = ???
    def update(id: Long, computer: Computer) = ???
  }

  object Company {
    def options: Seq[(String,String)] = ???
  }

  object html {
    def editForm(id: Long, f: Form[Computer], options: Seq[(String,String)]): String = ???
  }


  //#form-edit
  def edit(id: Long) = Action {
    Computer.findById(id).map { computer =>
      Ok(html.editForm(id, Form.fill(computer), Company.options))
    }.getOrElse(NotFound)
  }
  //#form-edit

  //#form-update
  def update(id: Long) = Action(parse.urlFormEncoded) { implicit request =>
    val r = computerValidation.validate(request.body)
    r match {
      case Failure(_) => BadRequest(html.editForm(id, Form(request.body, r), Company.options))
      case Success(computer) => {
        Computer.update(id, computer)
        Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
      }
    }
  }
  //#form-update

  "Scala Validation Form" should {
    "compile" in {
      success
    }
  }
}
