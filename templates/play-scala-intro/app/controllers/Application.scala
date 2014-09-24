package controllers

import play.api._
import play.api.mvc._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import models._
import javax.inject._

class Application @Inject() (db: DB) extends Controller {

  def index = Action {
    Ok(views.html.index())
  }

  val personForm: Form[Person] = Form {
  	mapping(
      "name" -> text
  	)(Person.apply)(Person.unapply)
  }

  def addPerson = Action { implicit request =>
  	val person = personForm.bindFromRequest.get
  	db.save(person)
  	Redirect(routes.Application.index)
  }

  def getPersons = Action {
  	val persons = db.query[Person].fetch()
  	Ok(Json.toJson(persons))
  }
}
