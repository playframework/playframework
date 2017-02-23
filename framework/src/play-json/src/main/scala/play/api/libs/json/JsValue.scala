/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.json

import scala.collection._

import play.api.data.validation.ValidationError

case class JsResultException(errors: Seq[(JsPath, Seq[ValidationError])]) extends RuntimeException("JsResultException(errors:%s)".format(errors))

/**
 * Generic json value
 */
sealed trait JsValue extends JsReadable {
  override def toString = Json.stringify(this)

  def validate[A](implicit rds: Reads[A]): JsResult[A] = rds.reads(this)

  def validateOpt[A](implicit rds: Reads[A]): JsResult[Option[A]] = JsDefined(this).validateOpt[A]
}

object JsValue {
  import scala.language.implicitConversions
  implicit def jsValueToJsLookup(value: JsValue): JsLookup = JsLookup(JsDefined(value))
}

/**
 * Represents a Json null value.
 */
case object JsNull extends JsValue

/**
 * Represent a Json boolean value.
 */
case class JsBoolean(value: Boolean) extends JsValue

/**
 * Represent a Json number value.
 */
case class JsNumber(value: BigDecimal) extends JsValue

/**
 * Represent a Json string value.
 */
case class JsString(value: String) extends JsValue

/**
 * Represent a Json array value.
 */
case class JsArray(value: Seq[JsValue] = List()) extends JsValue {

  /**
   * Concatenates this array with the elements of an other array.
   */
  def ++(other: JsArray): JsArray =
    JsArray(value ++ other.value)

  /**
   * Append an element to this array.
   */
  def :+(el: JsValue): JsArray = JsArray(value :+ el)
  def append(el: JsValue): JsArray = this.:+(el)

  /**
   * Prepend an element to this array.
   */
  def +:(el: JsValue): JsArray = JsArray(el +: value)
  def prepend(el: JsValue): JsArray = this.+:(el)

}

/**
 * Represent a Json object value.
 */
case class JsObject(private val underlying: Map[String, JsValue]) extends JsValue {

  /**
   * The fields of this JsObject in the order passed to to constructor
   */
  lazy val fields: Seq[(String, JsValue)] = underlying.toSeq

  /**
   * The value of this JsObject as an immutable map.
   */
  lazy val value: Map[String, JsValue] = underlying match {
    case m: immutable.Map[String, JsValue] => m
    case m => m.toMap
  }

  /**
   * Return all fields as a set
   */
  def fieldSet: Set[(String, JsValue)] = fields.toSet

  /**
   * Return all keys
   */
  def keys: Set[String] = underlying.keySet

  /**
   * Return all values
   */
  def values: Iterable[JsValue] = underlying.values

  /**
   * Merge this object with another one. Values from other override value of the current object.
   */
  def ++(other: JsObject): JsObject = JsObject(underlying ++ other.underlying)

  /**
   * Removes one field from the JsObject
   */
  def -(otherField: String): JsObject = JsObject(underlying - otherField)

  /**
   * Adds one field to the JsObject
   */
  def +(otherField: (String, JsValue)): JsObject = JsObject(underlying + otherField)

  /**
   * merges everything in depth and doesn't stop at first level, as ++ does
   */
  def deepMerge(other: JsObject): JsObject = {
    def merge(existingObject: JsObject, otherObject: JsObject): JsObject = {
      val result = existingObject.underlying ++ otherObject.underlying.map {
        case (otherKey, otherValue) =>
          val maybeExistingValue = existingObject.underlying.get(otherKey)

          val newValue = (maybeExistingValue, otherValue) match {
            case (Some(e: JsObject), o: JsObject) => merge(e, o)
            case _ => otherValue
          }
          otherKey -> newValue
      }
      JsObject(result)
    }
    merge(this, other)
  }

  override def equals(other: Any): Boolean = other match {
    case that: JsObject => (that canEqual this) && fieldSet == that.fieldSet
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[JsObject]

  override def hashCode: Int = fieldSet.hashCode()
}

object JsObject {
  /**
   * Construct a new JsObject, with the order of fields in the Seq.
   */
  def apply(fields: Seq[(String, JsValue)]): JsObject = new JsObject(mutable.LinkedHashMap(fields: _*))
}
