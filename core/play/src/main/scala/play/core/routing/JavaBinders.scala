/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.routing

import java.util.Optional
import java.util.OptionalDouble
import java.util.OptionalInt
import java.util.OptionalLong
import java.util.UUID

import scala.reflect.ClassTag

import controllers.Assets.Asset
import play.api.mvc.JavascriptLiteral
import play.api.mvc.PathBindable
import play.api.mvc.QueryStringBindable

/**
 * The binders and JavaScript literals of the routers that the routes compiler generates as Java source code.
 *
 * Generated Scala routers look them up implicitly. Generated Java routers get them from here instead, picked by the
 * parameter types in the routes file. The Java compiler then checks that the binder fits the type of the parameter.
 */
object JavaBinders {
  val pathString: PathBindable[String]                 = PathBindable.bindableString
  val pathCharacter: PathBindable[java.lang.Character] = PathBindable.bindableCharacter
  val pathShort: PathBindable[java.lang.Short]         = PathBindable.bindableJavaShort
  val pathInteger: PathBindable[java.lang.Integer]     = PathBindable.bindableJavaInteger
  val pathLong: PathBindable[java.lang.Long]           = PathBindable.bindableJavaLong
  val pathFloat: PathBindable[java.lang.Float]         = PathBindable.bindableJavaFloat
  val pathDouble: PathBindable[java.lang.Double]       = PathBindable.bindableJavaDouble
  val pathBoolean: PathBindable[java.lang.Boolean]     = PathBindable.bindableJavaBoolean
  val pathUUID: PathBindable[UUID]                     = PathBindable.bindableUUID

  /**
   * The binder of assets, which needs the fixed path parameter of the route to find the versioned asset.
   */
  def pathAsset(context: ReverseRouteContext): PathBindable[Asset] = Asset.assetPathBindable(context)

  /**
   * The binder of a type that implements [[play.mvc.PathBindable]].
   */
  def path[T <: play.mvc.PathBindable[T]](clazz: Class[T]): PathBindable[T] =
    PathBindable.javaPathBindable[T](ClassTag(clazz))

  val queryString: QueryStringBindable[String]                 = QueryStringBindable.bindableString
  val queryCharacter: QueryStringBindable[java.lang.Character] = QueryStringBindable.bindableCharacter
  val queryShort: QueryStringBindable[java.lang.Short]         = QueryStringBindable.bindableJavaShort
  val queryInteger: QueryStringBindable[java.lang.Integer]     = QueryStringBindable.bindableJavaInteger
  val queryLong: QueryStringBindable[java.lang.Long]           = QueryStringBindable.bindableJavaLong
  val queryFloat: QueryStringBindable[java.lang.Float]         = QueryStringBindable.bindableJavaFloat
  val queryDouble: QueryStringBindable[java.lang.Double]       = QueryStringBindable.bindableJavaDouble
  val queryBoolean: QueryStringBindable[java.lang.Boolean]     = QueryStringBindable.bindableJavaBoolean
  val queryUUID: QueryStringBindable[UUID]                     = QueryStringBindable.bindableUUID
  val queryOptionalInt: QueryStringBindable[OptionalInt]       = QueryStringBindable.bindableJavaOptionalInt
  val queryOptionalLong: QueryStringBindable[OptionalLong]     = QueryStringBindable.bindableJavaOptionalLong
  val queryOptionalDouble: QueryStringBindable[OptionalDouble] = QueryStringBindable.bindableJavaOptionalDouble

  def queryOptional[T](element: QueryStringBindable[T]): QueryStringBindable[Optional[T]] = {
    implicit val elementBinder: QueryStringBindable[T] = element
    QueryStringBindable.bindableJavaOption[T]
  }

  def queryList[T](element: QueryStringBindable[T]): QueryStringBindable[java.util.List[T]] = {
    implicit val elementBinder: QueryStringBindable[T] = element
    QueryStringBindable.bindableJavaList[T]
  }

  /**
   * The binder of a type that implements [[play.mvc.QueryStringBindable]].
   */
  def query[T <: play.mvc.QueryStringBindable[T]](clazz: Class[T]): QueryStringBindable[T] =
    QueryStringBindable.javaQueryStringBindable[T](ClassTag(clazz))

  def javascriptLiteral(value: String): String              = JavascriptLiteral.literalString.to(value)
  def javascriptLiteral(value: java.lang.Character): String = JavascriptLiteral.literalJavaCharacter.to(value)
  def javascriptLiteral(value: java.lang.Short): String     = JavascriptLiteral.literalJavaShort.to(value)
  def javascriptLiteral(value: java.lang.Integer): String   = JavascriptLiteral.literalJavaInteger.to(value)
  def javascriptLiteral(value: java.lang.Long): String      = JavascriptLiteral.literalJavaLong.to(value)
  def javascriptLiteral(value: java.lang.Float): String     = JavascriptLiteral.literalJavaFloat.to(value)
  def javascriptLiteral(value: java.lang.Double): String    = JavascriptLiteral.literalJavaDouble.to(value)
  def javascriptLiteral(value: java.lang.Boolean): String   = JavascriptLiteral.literalJavaBoolean.to(value)
  def javascriptLiteral(value: UUID): String                = JavascriptLiteral.literalUUID.to(value)
  def javascriptLiteral(value: Asset): String               = JavascriptLiteral.literalAsset.to(value)
  def javascriptLiteral(value: OptionalInt): String         = JavascriptLiteral.literalJavaOptionalInt.to(value)
  def javascriptLiteral(value: OptionalLong): String        = JavascriptLiteral.literalJavaOptionalLong.to(value)
  def javascriptLiteral(value: OptionalDouble): String      = JavascriptLiteral.literalJavaOptionalDouble.to(value)

  /**
   * The JavaScript literal of an optional value, which picks the literal of the value by its class, because Java
   * erases the type of the value.
   */
  def javascriptLiteral(value: Optional[?]): String = value.map[String](literalOf).orElse("null")

  private def literalOf(value: Any): String = value match {
    case v: String              => javascriptLiteral(v)
    case v: java.lang.Character => javascriptLiteral(v)
    case v: java.lang.Short     => javascriptLiteral(v)
    case v: java.lang.Integer   => javascriptLiteral(v)
    case v: java.lang.Long      => javascriptLiteral(v)
    case v: java.lang.Float     => javascriptLiteral(v)
    case v: java.lang.Double    => javascriptLiteral(v)
    case v: java.lang.Boolean   => javascriptLiteral(v)
    case v: UUID                => javascriptLiteral(v)
    case v: Asset               => javascriptLiteral(v)
    case v                      => throw new IllegalArgumentException(s"No JavaScript literal for ${v.getClass}")
  }
}
