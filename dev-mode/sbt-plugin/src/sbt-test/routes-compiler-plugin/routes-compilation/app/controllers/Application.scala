/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._
import javax.inject.Inject
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._
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
  def takeIntEscapes(i: Int) = Action {
    Ok(s"$i")
  }
  def takeBool(b: Boolean) = Action {
    Ok(s"$b")
  }
  def takeBool2(b: Boolean) = Action {
    Ok(s"$b")
  }
  def takeString(x: String) = Action {
    Ok(x)
  }
  def takeStringOption(x: Option[String]) = Action {
    Ok(x.getOrElse("emptyOption"))
  }
  def takeStringOptional(x: java.util.Optional[String]) = Action {
    Ok(x.asScala.getOrElse("emptyOptional"))
  }
  def takeChar(x: Char) = Action {
    Ok(x.toString)
  }
  def takeCharOption(x: Option[Char]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeInt(x: Int) = Action {
    Ok(x.toString)
  }
  def takeIntOption(x: Option[Int]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeInteger(x: Integer) = Action {
    Ok(x.toString)
  }
  def takeIntegerOptional(x: java.util.Optional[Integer]) = Action {
    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeListString(x: List[String]) = Action {
    Ok(x.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeListStringOption(x: Option[List[String]]) = Action {
    Ok(x.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
  }
  def takeListChar(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOption(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListString(x: java.util.List[String]) = Action {
    Ok(x.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeJavaListStringOptional(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(x.asScala.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
  }
  def takeListInt(x: List[Int]) = Action {
    Ok(x.mkString(","))
  }
  def takeListIntOption(x: Option[List[Int]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListInteger(x: java.util.List[Integer]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListIntegerOptional(x: java.util.Optional[java.util.List[Integer]]) = Action {
    Ok(x.asScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
  }
  def takeStringWithDefault(x: String) = Action {
    Ok(x)
  }
  def takeStringOptionWithDefault(x: Option[String]) = Action {
    Ok(x.getOrElse("emptyOption"))
  }
  def takeStringOptionalWithDefault(x: java.util.Optional[String]) = Action {
    Ok(x.asScala.getOrElse("emptyOptional"))
  }
  def takeCharWithDefault(x: Char) = Action {
    Ok(x.toString)
  }
  def takeCharOptionWithDefault(x: Option[Char]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeIntWithDefault(x: Int) = Action {
    Ok(x.toString)
  }
  def takeIntOptionWithDefault(x: Option[Int]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeIntegerWithDefault(x: Integer) = Action {
    Ok(x.toString)
  }
  def takeIntegerOptionalWithDefault(x: java.util.Optional[Integer]) = Action {
    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeListStringWithDefault(x: List[String]) = Action {
    Ok(x.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeListStringOptionWithDefault(x: Option[List[String]]) = Action {
    Ok(x.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
  }
  def takeListCharWithDefault(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOptionWithDefault(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListStringWithDefault(x: java.util.List[String]) = Action {
    Ok(x.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeJavaListStringOptionalWithDefault(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(x.asScala.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
  }
  def takeListIntWithDefault(x: List[Int]) = Action {
    Ok(x.mkString(","))
  }
  def takeListIntOptionWithDefault(x: Option[List[Int]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListIntegerWithDefault(x: java.util.List[Integer]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListIntegerOptionalWithDefault(x: java.util.Optional[java.util.List[Integer]]) = Action {
    Ok(x.asScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
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
