package play.api.libs.json

/**
 * Helper functions to handle JsValues.
 */
object Json {

  /**
   * Parse a String representing a json, and return it as a JsValue.
   *
   * @param input a String to parse
   * @return the JsValue representing the string
   */
  def parse(input: String): JsValue = JacksonJson.parseJsValue(input)

  /**
   * Convert a JsValue to its string representation.
   *
   * @param json the JsValue to convert
   * @return a String with the json representation
   */
  def stringify(json: JsValue): String = JacksonJson.generateFromJsValue(json)

  /**
   * Provided a Reads implicit for its type is available, convert any object into a JsValue.
   *
   * @param o Value to convert in Json.
   */
  def toJson[T](o: T)(implicit tjs: Writes[T]): JsValue = tjs.writes(o)

  /**
   * Provided a Writes implicit for that type is available, convert a JsValue to any type.
   *
   * @param json Json value to transform as an instance of T.
   */
  def fromJson[T](json: JsValue)(implicit fjs: Reads[T]): JsResult[T] = fjs.reads(json)

  /**
   * Next is the trait that allows Simplified Json syntax :
   *
   * Example :
   * JsObject(Seq(
   *    "key1", JsString("value"),
   *    "key2" -> JsNumber(123),
   *    "key3" -> JsObject(Seq("key31" -> JsString("value31")))
   * ))
   * ====> Json.obj( "key1" -> "value", "key2" -> 123, "key3" -> obj("key31" -> "value31"))
   *
   * JsArray(JsString("value"), JsNumber(123), JsBoolean(true))
   * ====> Json.arr( "value", 123, true )
   *
   * There is an implicit conversion from any Type with a Json Writes to JsValueWrapper which is an empty trait that
   * shouldn't end into unexpected implicit conversions
   *
   * Something to note due to JsValueWrapper extending NotNull :
   *    - null or None will end into compiling error : use JsNull instead
   */
  sealed trait JsValueWrapper extends NotNull

  private case class JsValueWrapperImpl(field: JsValue) extends JsValueWrapper

  implicit def toJsFieldJsValueWrapper[T](field: T)(implicit w: Writes[T]): JsValueWrapper = JsValueWrapperImpl(w.writes(field))

  def obj(fields: (String, JsValueWrapper)*): JsObject = JsObject(fields.map(f => (f._1, f._2.asInstanceOf[JsValueWrapperImpl].field)))
  def arr(fields: JsValueWrapper*): JsArray = JsArray(fields.map(_.asInstanceOf[JsValueWrapperImpl].field))

}
