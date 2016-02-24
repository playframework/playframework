/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import play.api.data.validation.ValidationError

/**
 * A value representing the value at a particular JSON path, either an actual JSON node or undefined.
 */
case class JsLookup(result: JsLookupResult) extends AnyVal {

  /**
   * Access the head of this array.
   */
  def head: JsLookupResult = result match {
    case JsDefined(JsArray(head +: tail)) => JsDefined(head)
    case JsDefined(arr: JsArray) => JsUndefined("Cannot get head of " + arr)
    case JsDefined(o) => JsUndefined(o + " is not an array")
    case undef => undef
  }

  /**
   * Access the tail of this array.
   */
  def tail: JsLookupResult = result match {
    case JsDefined(JsArray(head +: tail)) => JsDefined(JsArray(tail))
    case JsDefined(arr: JsArray) => JsUndefined("Cannot get tail of " + arr)
    case JsDefined(o) => JsUndefined(o + " is not an array")
    case undef => undef
  }

  /**
   * Access the last element of this array.
   */
  def last: JsLookupResult = result match {
    case JsDefined(JsArray(values)) if values.nonEmpty => JsDefined(values.last)
    case JsDefined(arr: JsArray) => JsUndefined("Cannot get last element of " + arr)
    case JsDefined(o) => JsUndefined(o + " is not an array")
    case undef => undef
  }

  /**
   * Access a value of this array.
   *
   * @param index Element index.
   */
  def apply(index: Int): JsLookupResult = result match {
    case JsDefined(arr: JsArray) =>
      arr.value.lift(index).map(JsDefined.apply).getOrElse(JsUndefined("Array index out of bounds in " + arr))
    case JsDefined(o) =>
      JsUndefined(o + " is not an array")
    case undef => undef
  }

  /**
   * Access a value of this array.
   *
   * @param index Element index
   */
  def \(index: Int): JsLookupResult = apply(index)

  /**
   * Return the property corresponding to the fieldName, supposing we have a JsObject.
   *
   * @param fieldName the name of the property to look up
   * @return the resulting JsValue wrapped in a JsLookup. If the current node is not a JsObject or doesn't have the
   *         property, a JsUndefined will be returned.
   */
  def \(fieldName: String): JsLookupResult = result match {
    case JsDefined(obj: JsObject) =>
      obj.value.get(fieldName).map(JsDefined.apply)
        .getOrElse(JsUndefined("'" + fieldName + "' is undefined on object: " + obj))
    case JsDefined(o) =>
      JsUndefined(o + " is not an object")
    case undef => undef
  }

  /**
   * Look up fieldName in the current object and all descendants.
   *
   * @return the list of matching nodes
   */
  def \\(fieldName: String): Seq[JsValue] = result match {
    case JsDefined(obj: JsObject) =>
      obj.value.foldLeft(Seq[JsValue]())((o, pair) => pair match {
        case (key, value) if key == fieldName => o ++ (value +: (value \\ fieldName))
        case (_, value) => o ++ (value \\ fieldName)
      })
    case JsDefined(arr: JsArray) =>
      arr.value.flatMap(_ \\ fieldName)
    case _ => Seq.empty
  }
}

sealed trait JsLookupResult extends Any with JsReadable {
  /**
   * Tries to convert the node into a JsValue
   */
  def toOption: Option[JsValue] = this match {
    case JsDefined(v) => Some(v)
    case _ => None
  }
  def toEither: Either[ValidationError, JsValue] = this match {
    case JsDefined(v) => Right(v)
    case undef: JsUndefined => Left(undef.validationError)
  }
  def get: JsValue = toOption.get
  def getOrElse(v: => JsValue): JsValue = toOption.getOrElse(v)

  def validate[A](implicit rds: Reads[A]): JsResult[A] = this match {
    case JsDefined(v) => v.validate[A]
    case undef: JsUndefined => JsError(undef.validationError)
  }

  /**
   * If this result contains `JsNull` or is undefined, returns `JsSuccess(None)`.
   * Otherwise returns the result of validating as an `A` and wrapping the result in a `Some`.
   */
  def validateOpt[A](implicit rds: Reads[A]): JsResult[Option[A]] = this match {
    case JsUndefined() => JsSuccess(None)
    case JsDefined(a) => Reads.optionWithNull(rds).reads(a)
  }
}
object JsLookupResult {
  import scala.language.implicitConversions
  implicit def jsLookupResultToJsLookup(value: JsLookupResult): JsLookup = JsLookup(value)
}
/**
 * Wrapper for JsValue to represent an existing Json value.
 */
case class JsDefined(value: JsValue) extends AnyVal with JsLookupResult

/**
 * Represent a missing Json value.
 */
final class JsUndefined(err: => String) extends JsLookupResult {
  def error = err
  def validationError = ValidationError(error)
  override def toString = "JsUndefined(" + err + ")"
}

object JsUndefined {
  def apply(err: => String) = new JsUndefined(err)
  def unapply(o: Object): Boolean = o.isInstanceOf[JsUndefined]
}
