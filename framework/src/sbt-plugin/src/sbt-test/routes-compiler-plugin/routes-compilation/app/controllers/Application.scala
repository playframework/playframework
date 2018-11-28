/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
//  def takeCharOptional(x: java.util.Optional[Char]) = Action {
//    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
//  }
  def takeInt(x: Int) = Action {
    Ok(x.toString)
  }
  def takeIntOption(x: Option[Int]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
//  def takeIntOptional(x: java.util.Optional[Int]) = Action {
//    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
//  }
  def takeInteger(x: Integer) = Action {
    Ok(x.toString)
  }
//  def takeIntegerOption(x: Option[Integer]) = Action {
//    Ok(x.map(_.toString).getOrElse("emptyOption"))
//  }
  def takeIntegerOptional(x: java.util.Optional[Integer]) = Action {
    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeListString(x: List[String]) = Action {
    Ok(x.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeListStringOption(x: Option[List[String]]) = Action {
    Ok(x.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListStringOptional(x: java.util.Optional[List[String]]) = Action {
//    Ok(x.asScala.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
//  }
  def takeListChar(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOption(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListCharOptional(x: java.util.Optional[List[Char]]) = Action {
//    Ok(x.asScala.map(_.mkString(",")).getOrElse("emptyOptional"))
//  }
  def takeJavaListString(x: java.util.List[String]) = Action {
    Ok(x.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
//  def takeJavaListStringOption(x: Option[java.util.List[String]]) = Action {
//    Ok(x.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
//  }
  def takeJavaListStringOptional(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(x.asScala.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
  }
//  def takeJavaListChar(x: java.util.List[Char]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeJavaListCharOption(x: Option[java.util.List[Char]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeJavaListCharOptional(x: java.util.Optional[java.util.List[Char]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
  def takeListInt(x: List[Int]) = Action {
    Ok(x.mkString(","))
  }
  def takeListIntOption(x: Option[List[Int]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListIntOptional(x: java.util.Optional[List[Int]]) = Action {
//    Ok(x.asScala.map(_.mkString(",")).getOrElse("emptyOptional"))
//  }
//  def takeJavaListInt(x: java.util.List[Int]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeJavaListIntOption(x: Option[java.util.List[Int]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeJavaListIntOptional(x: java.util.Optional[java.util.List[Int]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeListInteger(x: List[Integer]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeListIntegerOption(x: Option[List[Integer]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeListIntegerOptional(x: java.util.Optional[List[Integer]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
  def takeJavaListInteger(x: java.util.List[Integer]) = Action {
    Ok(x.asScala.mkString(","))
  }
//  def takeJavaListIntegerOption(x: Option[java.util.List[Integer]]) = Action {
//    Ok(x.map(_.asScala.mkString(",")).getOrElse("emptyOption"))
//  }
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
//  def takeCharOptionalWithDefault(x: java.util.Optional[Char]) = Action {
//    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
//  }
  def takeIntWithDefault(x: Int) = Action {
    Ok(x.toString)
  }
  def takeIntOptionWithDefault(x: Option[Int]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
//  def takeIntOptionalWithDefault(x: java.util.Optional[Int]) = Action {
//    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
//  }
  def takeIntegerWithDefault(x: Integer) = Action {
    Ok(x.toString)
  }
//  def takeIntegerOptionWithDefault(x: Option[Integer]) = Action {
//    Ok(x.map(_.toString).getOrElse("emptyOption"))
//  }
  def takeIntegerOptionalWithDefault(x: java.util.Optional[Integer]) = Action {
    Ok(x.asScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeListStringWithDefault(x: List[String]) = Action {
    Ok(x.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
  def takeListStringOptionWithDefault(x: Option[List[String]]) = Action {
    Ok(x.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListStringOptionalWithDefault(x: java.util.Optional[List[String]]) = Action {
//    Ok(x.asScala.map(_.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
//  }
  def takeListCharWithDefault(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOptionWithDefault(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListCharOptionalWithDefault(x: java.util.Optional[List[Char]]) = Action {
//    Ok(x.asScala.map(_.mkString(",")).getOrElse("emptyOptional"))
//  }
  def takeJavaListStringWithDefault(x: java.util.List[String]) = Action {
    Ok(x.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(","))
  }
//  def takeJavaListStringOptionWithDefault(x: Option[java.util.List[String]]) = Action {
//    Ok(x.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOption"))
//  }
  def takeJavaListStringOptionalWithDefault(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(x.asScala.map(_.asScala.map(str => if(str.isEmpty){"emptyStringElement"}else{str}).mkString(",")).getOrElse("emptyOptional"))
  }
//  def takeJavaListCharWithDefault(x: java.util.List[Char]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeJavaListCharOptionWithDefault(x: Option[java.util.List[Char]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeJavaListCharOptionalWithDefault(x: java.util.Optional[java.util.List[Char]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
  def takeListIntWithDefault(x: List[Int]) = Action {
    Ok(x.mkString(","))
  }
  def takeListIntOptionWithDefault(x: Option[List[Int]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
//  def takeListIntOptionalWithDefault(x: java.util.Optional[List[Int]]) = Action {
//    Ok(x.asScala.map(_.mkString(",")).getOrElse("emptyOptional"))
//  }
//  def takeJavaListIntWithDefault(x: java.util.List[Int]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeJavaListIntOptionWithDefault(x: Option[java.util.List[Int]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeJavaListIntOptionalWithDefault(x: java.util.Optional[java.util.List[Int]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
//  def takeListIntegerWithDefault(x: List[Integer]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeListIntegerOptionWithDefault(x: Option[List[Integer]]) = Action {
//    Ok(x.mkString(","))
//  }
//  def takeListIntegerOptionalWithDefault(x: java.util.Optional[List[Integer]]) = Action {
//    Ok(x.asScala.mkString(","))
//  }
  def takeJavaListIntegerWithDefault(x: java.util.List[Integer]) = Action {
    Ok(x.asScala.mkString(","))
  }
//  def takeJavaListIntegerOptionWithDefault(x: Option[java.util.List[Integer]]) = Action {
//    Ok(x.map(_.asScala.mkString(",")).getOrElse("emptyOption"))
//  }
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
