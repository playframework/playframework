/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.binder.controllers

import javax.inject.Inject

import play.api.mvc._
import scalaguide.binder.models._

class BinderApplication @Inject() (components: ControllerComponents) extends AbstractController(components) {
  // #path
  def user(user: User) = Action {
    Ok(user.name)
  }
  // #path

  // #query
  def age(age: AgeRange) = Action {
    Ok(age.from.toString)
  }
  // #query
}
