package controllers

import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.libs.functional.syntax._

import anorm.{ Pk, NotAssigned }

import views._
import models._

/**
 * Manage a database of computers
 */
object Application extends Controller {

  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list(0, 2, ""))

  implicit val computerValidation = From[UrlFormEncoded] { __ =>
    import play.api.data.mapping.Rules._
    ((__ \ "id").read(ignored(NotAssigned:Pk[Long])) ~
     (__ \ "name").read(notEmpty) ~
     (__ \ "introduced").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "discontinued").read(option(date("yyyy-MM-dd"))) ~
     (__ \ "company").read[Option[Long]]) (Computer.apply _)
  }

  implicit def pkW[I, O](implicit w: Path => Write[Option[I], O]) =
    (p: Path) => w(p).contramap((_: Pk[I]).toOption)

  implicit val computerW = To[UrlFormEncoded] { __ =>
    import play.api.data.mapping.Writes._
    ((__ \ "id").write[Pk[Long]] ~
     (__ \ "name").write[String] ~
     (__ \ "introduced").write(option(date("yyyy-MM-dd"))) ~
     (__ \ "discontinued").write(option(date("yyyy-MM-dd"))) ~
     (__ \ "company").write[Option[Long]]) (unlift(Computer.unapply _))
  }

  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */
  def index = Action { Home }

  /**
   * Display the paginated list of computers.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on computer names
   */
  def list(page: Int, orderBy: Int, filter: String) = Action { implicit request =>
    Ok(html.list(
      Computer.list(page = page, orderBy = orderBy, filter = ("%"+filter+"%")),
      orderBy, filter
    ))
  }

  /**
   * Display the 'edit form' of a existing Computer.
   *
   * @param id Id of the computer to edit
   */
  def edit(id: Long) = Action {
    Computer.findById(id).map { computer =>
      Ok(html.editForm(id, Form.fill(computer), Company.options))
    }.getOrElse(NotFound)
  }

  /**
   * Handle the 'edit form' submission
   *
   * @param id Id of the computer to edit
   */
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

  /**
   * Display the 'new computer form'.
   */
  def create = Action {
    Ok(html.createForm(Form(), Company.options))
  }

  /**
   * Handle the 'new computer form' submission.
   */
  def save = Action(parse.urlFormEncoded) { implicit request =>
    val r = computerValidation.validate(request.body)
    r match {
      case Failure(_) => BadRequest(html.createForm(Form(request.body, r), Company.options))
      case Success(computer) => {
        Computer.insert(computer)
        Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
      }
    }
  }

  /**
   * Handle computer deletion.
   */
  def delete(id: Long) = Action {
    Computer.delete(id)
    Home.flashing("success" -> "Computer has been deleted")
  }

}

