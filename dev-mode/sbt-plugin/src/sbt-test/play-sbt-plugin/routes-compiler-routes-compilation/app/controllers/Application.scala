/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import javax.inject.Inject

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

import models._
import play.api.mvc._

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
    Ok(x.toScala.getOrElse("emptyOptional"))
  }
  def takeChar(x: Char) = Action {
    Ok(x.toString)
  }
  def takeCharOption(x: Option[Char]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeCharacter(x: Character) = Action {
    Ok(x.toString)
  }
  def takeCharacterOptional(x: java.util.Optional[Character]) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOption"))
  }
  def takeShort(x: Short) = Action {
    Ok(x.toString)
  }
  def takeShortOption(x: Option[Short]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeJavaShort(x: java.lang.Short) = Action {
    Ok(x.toString)
  }
  def takeJavaShortOptional(x: java.util.Optional[java.lang.Short]) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptional"))
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
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeOptionalInt(x: java.util.OptionalInt) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalInt"))
  }
  def takeOptionalLong(x: java.util.OptionalLong) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalLong"))
  }
  def takeOptionalDouble(x: java.util.OptionalDouble) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalDouble"))
  }
  def takeListString(x: List[String]) = Action {
    Ok(
      x.map(str =>
        if (str.isEmpty) {
          "emptyStringElement"
        } else {
          str
        }
      ).mkString(",")
    )
  }
  def takeListStringOption(x: Option[List[String]]) = Action {
    Ok(
      x.map(
        _.map(str =>
          if (str.isEmpty) {
            "emptyStringElement"
          } else {
            str
          }
        ).mkString(",")
      ).getOrElse("emptyOption")
    )
  }
  def takeListChar(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOption(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListCharacter(x: java.util.List[Character]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListCharacterOptional(x: java.util.Optional[java.util.List[Character]]) = Action {
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
  }
  def takeJavaListString(x: java.util.List[String]) = Action {
    Ok(
      x.asScala
        .map(str =>
          if (str.isEmpty) {
            "emptyStringElement"
          } else {
            str
          }
        )
        .mkString(",")
    )
  }
  def takeJavaListStringOptional(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(
      x.toScala
        .map(
          _.asScala
            .map(str =>
              if (str.isEmpty) {
                "emptyStringElement"
              } else {
                str
              }
            )
            .mkString(",")
        )
        .getOrElse("emptyOptional")
    )
  }
  def takeListShort(x: List[Short]) = Action {
    Ok(x.mkString(","))
  }
  def takeListShortOption(x: Option[List[Short]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListShort(x: java.util.List[java.lang.Short]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListShortOptional(x: java.util.Optional[java.util.List[java.lang.Short]]) = Action {
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
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
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
  }
  def takeStringWithDefault(x: String) = Action {
    Ok(x)
  }
  def takeStringOptionWithDefault(x: Option[String]) = Action {
    Ok(x.getOrElse("emptyOption"))
  }
  def takeStringOptionalWithDefault(x: java.util.Optional[String]) = Action {
    Ok(x.toScala.getOrElse("emptyOptional"))
  }
  def takeCharWithDefault(x: Char) = Action {
    Ok(x.toString)
  }
  def takeCharOptionWithDefault(x: Option[Char]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeCharacterWithDefault(x: Character) = Action {
    Ok(x.toString)
  }
  def takeCharacterOptionalWithDefault(x: java.util.Optional[Character]) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOption"))
  }
  def takeShortWithDefault(x: Short) = Action {
    Ok(x.toString)
  }
  def takeShortOptionWithDefault(x: Option[Short]) = Action {
    Ok(x.map(_.toString).getOrElse("emptyOption"))
  }
  def takeJavaShortWithDefault(x: java.lang.Short) = Action {
    Ok(x.toString)
  }
  def takeJavaShortOptionalWithDefault(x: java.util.Optional[java.lang.Short]) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptional"))
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
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptional"))
  }
  def takeOptionalIntWithDefault(x: java.util.OptionalInt) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalInt"))
  }
  def takeOptionalLongWithDefault(x: java.util.OptionalLong) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalLong"))
  }
  def takeOptionalDoubleWithDefault(x: java.util.OptionalDouble) = Action {
    Ok(x.toScala.map(_.toString).getOrElse("emptyOptionalDouble"))
  }
  def takeListStringWithDefault(x: List[String]) = Action {
    Ok(
      x.map(str =>
        if (str.isEmpty) {
          "emptyStringElement"
        } else {
          str
        }
      ).mkString(",")
    )
  }
  def takeListStringOptionWithDefault(x: Option[List[String]]) = Action {
    Ok(
      x.map(
        _.map(str =>
          if (str.isEmpty) {
            "emptyStringElement"
          } else {
            str
          }
        ).mkString(",")
      ).getOrElse("emptyOption")
    )
  }
  def takeListCharWithDefault(x: List[Char]) = Action {
    Ok(x.mkString(","))
  }
  def takeListCharOptionWithDefault(x: Option[List[Char]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListCharacterWithDefault(x: java.util.List[Character]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListCharacterOptionalWithDefault(x: java.util.Optional[java.util.List[Character]]) = Action {
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListStringWithDefault(x: java.util.List[String]) = Action {
    Ok(
      x.asScala
        .map(str =>
          if (str.isEmpty) {
            "emptyStringElement"
          } else {
            str
          }
        )
        .mkString(",")
    )
  }
  def takeJavaListStringOptionalWithDefault(x: java.util.Optional[java.util.List[String]]) = Action {
    Ok(
      x.toScala
        .map(
          _.asScala
            .map(str =>
              if (str.isEmpty) {
                "emptyStringElement"
              } else {
                str
              }
            )
            .mkString(",")
        )
        .getOrElse("emptyOptional")
    )
  }
  def takeListShortWithDefault(x: List[Short]) = Action {
    Ok(x.mkString(","))
  }
  def takeListShortOptionWithDefault(x: Option[List[Short]]) = Action {
    Ok(x.map(_.mkString(",")).getOrElse("emptyOption"))
  }
  def takeJavaListShortWithDefault(x: java.util.List[java.lang.Short]) = Action {
    Ok(x.asScala.mkString(","))
  }
  def takeJavaListShortOptionalWithDefault(x: java.util.Optional[java.util.List[java.lang.Short]]) = Action {
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
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
    Ok(x.toScala.map(_.asScala.mkString(",")).getOrElse("emptyOptional"))
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
  def fixedValue(parameter: String) = Action {
    Ok(parameter)
  }
  def hello = Action {
    Ok("Hello world!")
  }
  def interpolatorWarning(parameter: String) = Action {
    Ok(parameter)
  }
}
