/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package controllers

import play.api.mvc._
import scala.collection.JavaConversions._

object Application extends Controller {
  def index = Action {
    Ok
  }
  def post = Action {
    Ok
  }
  def withParam(param: String) = Action {
    Ok(param)
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
    Ok(x.mkString(","))
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
}
