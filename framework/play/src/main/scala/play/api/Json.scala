package play.api

import dispatch.json.{ JsValue, JsObject, JsArray, JsString, JsNumber, JsNull }
import sjson.json.{ Reads, Writes, Format }

object Json {

  // Extend the JsValue and JsObject API for easier parsing

  implicit def dispatchJsObject2playJsObject(jsobject: JsObject) = PlayJsObject(jsobject)
  implicit def dispatchJsValue2playJsValue(jsvalue: JsValue) = PlayJsValue(jsvalue)

  case class PlayJsValue(protected val value: JsValue) {
    import scala.util.control.Exception.catching

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Throws an exception if the JsValue can not be converted into this type.
     */
    def as[T](implicit fjs: Reads[T]): T = asOpt[T].get

    /**
     * Convert the JsValue to a scala object. T can be any type with a Reads[T] available.
     * Return None if the JsValue doesn't correspond to the expected type.
     */
    def asOpt[T](implicit fjs: Reads[T]): Option[T] = value match {
      case JsNull => None
      case _ => catching(classOf[RuntimeException]).opt(fjs.reads(value))
    }

    /**
     * Supposes that the current JsValue is an object and tries to extract a given key.
     * Throws if the JsValue is not an object.
     */
    def \(key: String): JsValue = value match {
      case JsObject(m) => m(JsString(key))
      case _ => throw new RuntimeException(value + " is not a JsObject")
    }

  }

  case class PlayJsObject(protected override val value: JsObject) extends PlayJsValue(value) {
    def apply(key: String): JsValue = value.self(JsString(key))
    def get(key: String): Option[JsValue] = value.self.get(JsString(key))
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
