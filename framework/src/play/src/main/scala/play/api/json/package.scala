package play.api.json

object `package` {

  /**
   * Parse a String representing a json, and return it as a JsValue
   * @param input a String to parse
   * @return the JsValue representing the strgin
   */
  def parseJson(input: String): JsValue = JerksonJson.parse[JsValue](input)

  /**
   * Convert a JsValue to its string representation.
   * @param json the JsValue to convert
   * @return a String with the json representation
   */
  def stringify(json: JsValue): String = JerksonJson.generate(json)

  implicit def intReader(json: JsValue) = json match {
    case JsNumber(n) => n.toInt
    case _ => throw new RuntimeException("Int expected")
  }
  implicit def shortReader(json: JsValue) = json match {
    case JsNumber(n) => n.toShort
    case _ => throw new RuntimeException("Short expected")
  }
  implicit def longReader(json: JsValue) = json match {
    case JsNumber(n) => n.toLong
    case _ => throw new RuntimeException("Long expected")
  }
  implicit def floatReader(json: JsValue) = json match {
    case JsNumber(n) => n.toFloat
    case _ => throw new RuntimeException("Float expected")
  }
  implicit def doubleReader(json: JsValue) = json match {
    case JsNumber(n) => n.toDouble
    case _ => throw new RuntimeException("Double expected")
  }
  implicit def booleanReader(json: JsValue) = json match {
    case JsBoolean(v) => v
    case _ => throw new RuntimeException("Boolean expected")
  }
  implicit def stringReader(json: JsValue) = json match {
    case JsString(v) => v
    case _ => throw new RuntimeException("String expected")
  }

  /* XXX Scala compiler doesn't consider generic methods for implicit conversion, so you have to
   * define the implicit conversion yourself. For example:
   *
   * implicit def myTypeListReader(json: JsValue): List[MyType] = listReader(json)
   */
  def listReader[T](json: JsValue)(implicit f: JsValue => T): List[T] = json match {
    case JsArray(ts) => ts map f
    case _ => throw new RuntimeException("List expected")
  }
  def mapReader[K, V](json: JsValue)(implicit kr: JsValue => K, vr: JsValue => V) = json match {
    case JsObject(m) => Map() ++ m map { case (k, v) => (kr(JsString(k)), vr(v)) }
    case _ => throw new RuntimeException("Map expected")
  }

  implicit def intWriter(o: Int) = JsNumber(o)
  implicit def shortWriter(o: Short) = JsNumber(o)
  implicit def longWriter(o: Long) = JsNumber(o)
  implicit def floatWriter(o: Float) = JsNumber(o)
  implicit def doubleWriter(o: Double) = JsNumber(o)
  implicit def booleanWriter(o: Boolean) = JsBoolean(o)
  implicit def stringWriter(o: String) = JsString(o)
  implicit def listWriter[T](o: List[T])(implicit f: T => JsValue) = JsArray(o map f)
  implicit def mapWriter[K, V](o: Map[K, V])(implicit kw: K => JsValue, vw: V => JsValue) =
    JsObject(o map { case (k, v) => (k.toString, vw(v)) })
}
