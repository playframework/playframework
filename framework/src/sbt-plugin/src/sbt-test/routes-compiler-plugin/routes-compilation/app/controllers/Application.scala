/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._
import javax.inject.Inject
import scala.collection.JavaConverters._
import models._

class Application @Inject() (c: ControllerComponents) extends AbstractController(c) {
  def index = Action {
    Ok
  }
  def post = Action {
    Ok
  }
  def withParam(param: String) = Action {
    Ok(param)
  }
  def user(userId: UserId) = Action {
    Ok(userId.id)
  }
  def queryUser(userId: UserId) = Action {
    Ok(userId.id)
  }
  def takeInt(i: Int) = Action {
    Ok(s"$i")
  }
  def takeBool(b: Boolean) = Action {
    Ok(s"$b")
  }
  def takeBool2(b: Boolean) = Action {
    Ok(s"$b")
  }
  def takeList(x: List[Int]) = Action {
    Ok(x.mkString(","))
  }
  def takeJavaList(x: java.util.List[Integer]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def urlcoding(dynamic: String, static: String, query: String) = Action {
    Ok(s"dynamic=$dynamic static=$static query=$query")
  }
  def route(parameter: String) = Action {
    Ok(parameter)
  }
  def routetest(parameter: String) = Action {
    Ok(parameter)
  }
  def routedefault(parameter: String) = Action {
    Ok(parameter)
  }
  def hello = Action {
    Ok("Hello world!")
  }
  def interpolatorWarning(parameter: String) = Action {
    Ok(parameter)
  }
}
