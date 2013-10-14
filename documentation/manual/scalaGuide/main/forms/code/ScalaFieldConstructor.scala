/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package scalaguide.forms.scalafieldconstructor {

import play.api.mvc._

package controllers {

import play.api.data.Form
import play.api.data.Forms._
import models.User

object Application extends Controller {

  def submit = Action {
    Ok("Welcome")
  }

  def login = Action {
    Ok("Welcome")
  }

  def home = Action{
    Ok("Welcome!")
  }

  val userForm = Form(
    mapping(
      "name" -> text,
      "password" -> text
    )(User.apply)(User.unapply)
  )
}
}
}

package models {
  case class User(name:String, password:String)
}

package controllers {

import javaguide.forms.html.myFieldConstructorTemplate

//#form-myfield-helper
object MyHelpers {
  import views.html.helper.FieldConstructor
  implicit val myFields = FieldConstructor(myFieldConstructorTemplate.f)
}
//#form-myfield-helper

}