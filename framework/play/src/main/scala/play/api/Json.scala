package play.api

import dispatch.json.{ JsValue, JsObject, JsArray, JsString, JsNumber, JsNull }
import sjson.json.{ Reads, Writes, Format }

object Json {

  // Extend the JsValue and JsObject API for easier parsing

  implicit def dispatchJsValue2playJsValue(jsvalue: JsValue) = PlayJsValue(jsvalue)

  sealed trait PlayJson {
    import scala.util.control.Exception.catching

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Throws an exception if the JsValue can not be converted into this type.
     */
    def as[T](implicit fjs: Reads[T]): T

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Return None if the JsValue doesn't correspond to the expected type.
     */
    def asOpt[T](implicit fjs: Reads[T]): Option[T]

    /**
     * Supposes that the current JsValue is an object and tries to extract a given key.
     * Throws if the JsValue is not an object.
     */
    def \(key: String): PlayJson

    def asValue: JsValue = this match {
      case PlayJsValue(v) => v
      case u @ PlayJsUndefined(msg) => throw new Exception(msg)
    }

  }

  case class PlayJsValue(protected val value: JsValue) extends PlayJson {
    import scala.util.control.Exception.catching

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Throws an exception if the JsValue can not be converted into this type.
     */
    def as[T](implicit fjs: Reads[T]): T = fjs.reads(value)

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Return None if the JsValue doesn't correspond to the expected type.
     */
    def asOpt[T](implicit fjs: Reads[T]): Option[T] = value match {
      case JsNull => None
      case value => catching(classOf[RuntimeException]).opt(fjs.reads(value))
    }

    /**
     * Supposes that the current JsValue is an object and tries to extract a given key.
     * Throws if the JsValue is not an object.
     */
    def \(key: String): PlayJson = value match {
      case JsObject(m) => m.get(JsString(key)).map(PlayJsValue(_)).getOrElse(PlayJsUndefined("'"+key+"'" + " is undefined on object: " + value))
      case other => PlayJsUndefined(key + " is undefined on value: " + value)
    }

  }

  case class PlayJsUndefined(error: String) extends PlayJson {

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Throws an exception if the JsValue can not be converted into this type.
     */
    def as[T](implicit fjs: Reads[T]): T = throw new Exception("undefined: " + error + ", cannot be viewed as the required type")

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Return None if the JsValue doesn't correspond to the expected type.
     */
    def asOpt[T](implicit fjs: Reads[T]): Option[T] = None

    /**
     * Supposes that the current JsValue is an object and tries to extract a given key.
     * Throws if the JsValue is not an object.
     */
    def \(key: String): PlayJson = this

  }

  /**
   * Helper for Json. Includes a simpler way to create heterogeneous JsObject or JsArray:
   *
   *  jsobject("id" -> 1,
   *           "name" -> "Toto",
   *           "asd" -> jsobject("asdf" -> true),
   *           "tutu" -> jslist("asdf", 4, true))
   *
   * Any type T that has Format[T] implicit imported can be used.
   */
  def jsobject(params: (String, JsValue)*): JsObject =
    JsObject(params.map(t => (JsString(t._1), t._2)))

  def jsarray(params: JsValue*): JsArray = JsArray(params.toList)

}
