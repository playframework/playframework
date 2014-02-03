package play.api.libs.json

import play.api.libs.iteratee.Execution.Implicits.defaultExecutionContext

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
   * Parse a byte array representing a json, and return it as a JsValue.
   *
   * The character encoding used will be automatically detected as UTF-8, UTF-16 or UTF-32, as per the heuristics in
   * RFC-4627.
   *
   * @param input a byte array to parse
   * @return the JsValue representing the byte array
   */
  def parse(input: Array[Byte]): JsValue = JacksonJson.parseJsValue(input)

  /**
   * Convert a JsValue to its string representation.
   *
   * {{{
   * scala> Json.stringify(Json.obj(
   *   "field1" -> Json.obj(
   *     "field11" -> "value11",
   *     "field12" -> Json.arr("alpha", 123L)
   *   )
   * ))
   * res0: String = {"field1":{"field11":"value11","field12":["alpha",123]}}
   *
   * scala> Json.stringify(res0)
   * res1: String = {"field1":{"field11":"value11","field12":["alpha",123]}}
   * }}}
   *
   * @param json the JsValue to convert
   * @return a String with the json representation
   */
  def stringify(json: JsValue): String = JacksonJson.generateFromJsValue(json)

  /**
   * Convert a JsValue to its pretty string representation using default Jackson
   * pretty printer (line feeds after each fields and 2-spaces indentation).
   *
   * {{{
   * scala> Json.stringify(Json.obj(
   *   "field1" -> Json.obj(
   *     "field11" -> "value11",
   *     "field12" -> Json.arr("alpha", 123L)
   *   )
   * ))
   * res0: String = {"field1":{"field11":"value11","field12":["alpha",123]}}
   *
   * scala> Json.prettyPrint(res0)
   * res1: String =
   * {
   *   "field1" : {
   *     "field11" : "value11",
   *     "field12" : [ "alpha", 123 ]
   *   }
   * }
   * }}}
   *
   * @param json the JsValue to convert
   * @return a String with the json representation
   */
  def prettyPrint(json: JsValue): String = JacksonJson.prettyPrint(json)

  /**
   * Provided a Writes implicit for its type is available, convert any object into a JsValue.
   *
   * @param o Value to convert in Json.
   */
  def toJson[T](o: T)(implicit tjs: Writes[T]): JsValue = tjs.writes(o)

  /**
   * Provided a Reads implicit for that type is available, convert a JsValue to any type.
   *
   * @param json Json value to transform as an instance of T.
   */
  def fromJson[T](json: JsValue)(implicit fjs: Reads[T]): JsResult[T] = fjs.reads(json)

  /**
   * Next is the trait that allows Simplified Json syntax :
   *
   * Example :
   * {{{
   * JsObject(Seq(
   *    "key1", JsString("value"),
   *    "key2" -> JsNumber(123),
   *    "key3" -> JsObject(Seq("key31" -> JsString("value31")))
   * )) == Json.obj( "key1" -> "value", "key2" -> 123, "key3" -> obj("key31" -> "value31"))
   *
   * JsArray(JsString("value"), JsNumber(123), JsBoolean(true)) == Json.arr( "value", 123, true )
   * }}}
   *
   * There is an implicit conversion from any Type with a Json Writes to JsValueWrapper
   * which is an empty trait that shouldn't end into unexpected implicit conversions.
   *
   * Something to note due to `JsValueWrapper` extending `NotNull` :
   * `null` or `None` will end into compiling error : use JsNull instead.
   */
  sealed trait JsValueWrapper extends NotNull

  private case class JsValueWrapperImpl(field: JsValue) extends JsValueWrapper

  import scala.language.implicitConversions

  implicit def toJsFieldJsValueWrapper[T](field: T)(implicit w: Writes[T]): JsValueWrapper = JsValueWrapperImpl(w.writes(field))

  def obj(fields: (String, JsValueWrapper)*): JsObject = JsObject(fields.map(f => (f._1, f._2.asInstanceOf[JsValueWrapperImpl].field)))
  def arr(fields: JsValueWrapper*): JsArray = JsArray(fields.map(_.asInstanceOf[JsValueWrapperImpl].field))

  import play.api.libs.iteratee.Enumeratee

  /**
   * Transform a stream of A to a stream of JsValue
   * {{{
   *   val fooStream: Enumerator[Foo] = ???
   *   val jsonStream: Enumerator[JsValue] = fooStream &> Json.toJson
   * }}}
   */
  def toJson[A: Writes]: Enumeratee[A, JsValue] = Enumeratee.map[A](Json.toJson(_))
  /**
   * Transform a stream of JsValue to a stream of A, keeping only successful results
   * {{{
   *   val jsonStream: Enumerator[JsValue] = ???
   *   val fooStream: Enumerator[Foo] = jsonStream &> Json.fromJson
   * }}}
   */
  def fromJson[A: Reads]: Enumeratee[JsValue, A] =
    Enumeratee.map[JsValue]((json: JsValue) => Json.fromJson(json)) ><> Enumeratee.collect[JsResult[A]] { case JsSuccess(value, _) => value }

  /**
   * Experimental JSON extensions to replace asProductXXX by generating
   * Reads[T]/Writes[T]/Format[T] from case class at COMPILE time using
   * new Scala 2.10 macro & reflection features.
   */
  import scala.reflect.macros.Context
  import language.experimental.macros

  /**
   * Creates a Reads[T] by resolving case class fields & required implcits at COMPILE-time.
   *
   * If any missing implicit is discovered, compiler will break with corresponding error.
   * {{{
   *   import play.api.libs.json.Json
   *
   *   case class User(name: String, age: Int)
   *
   *   implicit val userReads = Json.reads[User]
   *   // macro-compiler replaces Json.reads[User] by injecting into compile chain
   *   // the exact code you would write yourself. This is strictly equivalent to:
   *   implicit val userReads = (
   *      (__ \ 'name).read[String] and
   *      (__ \ 'age).read[Int]
   *   )(User)
   * }}}
   */
  def reads[A] = macro JsMacroImpl.readsImpl[A]

  /**
   * Creates a Writes[T] by resolving case class fields & required implcits at COMPILE-time
   *
   * If any missing implicit is discovered, compiler will break with corresponding error.
   * {{{
   *   import play.api.libs.json.Json
   *
   *   case class User(name: String, age: Int)
   *
   *   implicit val userWrites = Json.writes[User]
   *   // macro-compiler replaces Json.writes[User] by injecting into compile chain
   *   // the exact code you would write yourself. This is strictly equivalent to:
   *   implicit val userWrites = (
   *      (__ \ 'name).write[String] and
   *      (__ \ 'age).write[Int]
   *   )(unlift(User.unapply))
   * }}}
   */
  def writes[A] = macro JsMacroImpl.writesImpl[A]

  /**
   * Creates a Format[T] by resolving case class fields & required implicits at COMPILE-time
   *
   * If any missing implicit is discovered, compiler will break with corresponding error.
   * {{{
   *   import play.api.libs.json.Json
   *
   *   case class User(name: String, age: Int)
   *
   *   implicit val userWrites = Json.format[User]
   *   // macro-compiler replaces Json.format[User] by injecting into compile chain
   *   // the exact code you would write yourself. This is strictly equivalent to:
   *   implicit val userWrites = (
   *      (__ \ 'name).format[String] and
   *      (__ \ 'age).format[Int]
   *   )(User.apply, unlift(User.unapply))
   * }}}
   */
  def format[A] = macro JsMacroImpl.formatImpl[A]

}
