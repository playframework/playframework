package play.api.json

import org.codehaus.jackson.map.module.SimpleModule
import org.codehaus.jackson.Version
import org.codehaus.jackson.map.Module.SetupContext

import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import scala.collection.{ Traversable, MapLike, immutable, mutable }
import scala.collection.generic.{ MapFactory, GenericCompanion }

object `package` {

  def parseJson(input: String): JsValue = JerksonJson.parse[JsValue](input)

  def stringify(json: JsValue): String = JerksonJson.generate(json)

  def toJson[T](o: T)(implicit tjs: Writes[T]): JsValue = tjs.writes(o)

  def fromJson[T](json: JsValue)(implicit fjs: Reads[T]): T = fjs.reads(json)

}

private object JerksonJson extends com.codahale.jerkson.Json {

  object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
    override def setupModule(context: SetupContext) {
      context.addDeserializers(new PlayDeserializers(classLoader))
      context.addSerializers(new PlaySerializers)
    }
  }
  mapper.registerModule(module)

}

trait Writes[T] {
  def writes(o: T): JsValue
}

trait Reads[T] {
  def reads(json: JsValue): T
}

trait Format[T] extends Writes[T] with Reads[T]

object Reads {

  implicit object IntReads extends Reads[Int] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toInt
      case _ => throw new RuntimeException("Int expected")
    }
  }

  implicit object ShortReads extends Reads[Short] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toShort
      case _ => throw new RuntimeException("Short expected")
    }
  }

  implicit object LongReads extends Reads[Long] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toLong
      case _ => throw new RuntimeException("Long expected")
    }
  }

  implicit object FloatReads extends Reads[Float] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toFloat
      case _ => throw new RuntimeException("Float expected")
    }
  }

  implicit object DoubleReads extends Reads[Double] {
    def reads(json: JsValue) = json match {
      case JsNumber(n) => n.toDouble
      case _ => throw new RuntimeException("Double expected")
    }
  }

  implicit object BooleanReads extends Reads[Boolean] {
    def reads(json: JsValue) = json match {
      case JsBoolean(b) => b
      case _ => throw new RuntimeException("Boolean expected")
    }
  }

  implicit object StringReads extends Reads[String] {
    def reads(json: JsValue) = json match {
      case JsString(s) => s
      case _ => throw new RuntimeException("String expected")
    }
  }

  implicit def listReads[T](implicit fmt: Reads[T]): Reads[List[T]] = new Reads[List[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("List expected")
    }
  }

  implicit def seqReads[T](implicit fmt: Reads[T]): Reads[Seq[T]] = new Reads[Seq[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => ts.map(t => fromJson(t)(fmt))
      case _ => throw new RuntimeException("Seq expected")
    }
  }

  import scala.reflect.Manifest
  implicit def arrayReads[T](implicit fmt: Reads[T], mf: Manifest[T]): Reads[Array[T]] = new Reads[Array[T]] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => listToArray(ts.map(t => fromJson(t)(fmt)))
      case _ => throw new RuntimeException("Array expected")
    }
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit def mapReads[K, V](implicit fmtk: Reads[K], fmtv: Reads[V]): Reads[Map[K, V]] = new Reads[Map[K, V]] {
    def reads(json: JsValue) = json match {
      case JsObject(m) => Map() ++ m.map { case (k, v) => (fromJson[K](JsString(k))(fmtk), fromJson[V](v)(fmtv)) }
      case _ => throw new RuntimeException("Map expected")
    }
  }

  import scala.collection._
  implicit def mutableSetReads[T](implicit fmt: Reads[T]): Reads[mutable.Set[T]] =
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))

  implicit def immutableSetReads[T](implicit fmt: Reads[T]): Reads[immutable.Set[T]] =
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))

  implicit def immutableSortedSetReads[S](implicit ord: S => Ordered[S], binS: Reads[S]): Reads[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Reads[T]): Reads[S] = new Reads[S] {
    def reads(json: JsValue) = json match {
      case JsArray(ts) => f(ts.map(t => fromJson[T](t)))
      case _ => throw new RuntimeException("Collection expected")
    }
  }

  implicit object JsValueReads extends Reads[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }

}

object Writes {

  implicit object IntWrites extends Writes[Int] {
    def writes(o: Int) = JsNumber(o)
  }

  implicit object ShortWrites extends Writes[Short] {
    def writes(o: Short) = JsNumber(o)
  }

  implicit object LongWrites extends Writes[Long] {
    def writes(o: Long) = JsNumber(o)
  }

  implicit object FloatWrites extends Writes[Float] {
    def writes(o: Float) = JsNumber(o)
  }

  implicit object DoubleWrites extends Writes[Double] {
    def writes(o: Double) = JsNumber(o)
  }

  implicit object BooleanWrites extends Writes[Boolean] {
    def writes(o: Boolean) = JsBoolean(o)
  }

  implicit object StringWrites extends Writes[String] {
    def writes(o: String) = JsString(o)
  }

  implicit def listWrites[T](implicit fmt: Writes[T]): Writes[List[T]] = new Writes[List[T]] {
    def writes(ts: List[T]) = JsArray(ts.map(t => toJson(t)(fmt)))
  }

  implicit def seqWrites[T](implicit fmt: Writes[T]): Writes[Seq[T]] = new Writes[Seq[T]] {
    def writes(ts: Seq[T]) = JsArray(ts.toList.map(t => toJson(t)(fmt)))
  }

  import scala.reflect.Manifest
  implicit def arrayWrites[T](implicit fmt: Writes[T], mf: Manifest[T]): Writes[Array[T]] = new Writes[Array[T]] {
    def writes(ts: Array[T]) = JsArray((ts.map(t => toJson(t)(fmt))).toList)
  }
  def listToArray[T: Manifest](ls: List[T]): Array[T] = ls.toArray

  implicit def mapWrites[K, V](implicit fmtk: Writes[K], fmtv: Writes[V]): Writes[Map[K, V]] = new Writes[Map[K, V]] {
    def writes(ts: Map[K, V]) = JsObject(ts.map { case (k, v) => (k.toString, toJson(v)(fmtv)) })
  }

  import scala.collection._
  implicit def mutableSetWrites[T](implicit fmt: Writes[T]): Writes[mutable.Set[T]] =
    viaSeq((x: Seq[T]) => mutable.Set(x: _*))

  implicit def immutableSetWrites[T](implicit fmt: Writes[T]): Writes[immutable.Set[T]] =
    viaSeq((x: Seq[T]) => immutable.Set(x: _*))

  implicit def immutableSortedSetWrites[S](implicit ord: S => Ordered[S], binS: Writes[S]): Writes[immutable.SortedSet[S]] = {
    viaSeq((x: Seq[S]) => immutable.TreeSet[S](x: _*))
  }

  def viaSeq[S <: Iterable[T], T](f: Seq[T] => S)(implicit fmt: Writes[T]): Writes[S] = new Writes[S] {
    def writes(ts: S) = JsArray(ts.map(t => toJson(t)(fmt)).toList)
  }

  implicit object JsValueWrites extends Writes[JsValue] {
    def writes(o: JsValue) = o
    def reads(json: JsValue) = json
  }
}

// AST

sealed trait JsValue {
  import scala.util.control.Exception._

  def value: Any

  def valueAs[A]: A = value.asInstanceOf[A]

  def \(fieldName: String): JsValue = JsUndefined("'" + fieldName + "'" + " is undefined on object: " + this)

  def apply(idx: Int): JsValue = JsUndefined(this.toString + " is not an array")

  def \\(fieldName: String): Seq[JsValue] = Nil

  def asOpt[T](implicit fjs: Reads[T]): Option[T] = catching(classOf[RuntimeException]).opt(fjs.reads(this))

  def as[T](implicit fjs: Reads[T]): T = fjs.reads(this)

  override def toString = stringify(this)

}

case object JsNull extends JsValue {
  override def value = null
}

case class JsUndefined(error: String) extends JsValue {
  override def value = null
}

case class JsBoolean(override val value: Boolean) extends JsValue

case class JsNumber(override val value: BigDecimal) extends JsValue

case class JsString(override val value: String) extends JsValue

case class JsArray(override val value: List[JsValue]) extends JsValue {

  override def apply(index: Int): JsValue = {
    try {
      value(index)
    } catch {
      case _ => JsNull
    }
  }

  override def \\(fieldName: String): Seq[JsValue] = value.flatMap(_ \\ fieldName)

}

case class JsObject(override val value: Map[String, JsValue]) extends JsValue {

  override def \(fieldName: String): JsValue = value.get(fieldName).getOrElse(super.\(fieldName))

  override def \\(fieldName: String): Seq[JsValue] = {
    value.foldLeft(Seq[JsValue]())((o, pair) => pair match {
      case (key, value) if key == fieldName => o ++ (value +: (value \\ fieldName))
      case (_, value) => o ++ (value \\ fieldName)
    })
  }
}

import org.codehaus.jackson.{ JsonGenerator, JsonToken, JsonParser }
import org.codehaus.jackson.map.{ SerializerProvider, JsonSerializer, DeserializationContext, JsonDeserializer }
import org.codehaus.jackson.map.annotate.JsonCachable
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map.`type`.{ TypeFactory, ArrayType }

@JsonCachable
class JsValueSerializer extends JsonSerializer[JsValue] {

  def serialize(value: JsValue, json: JsonGenerator, provider: SerializerProvider) {
    value match {
      case JsNumber(v) => json.writeNumber(v.doubleValue())
      case JsString(v) => json.writeString(v)
      case JsBoolean(v) => json.writeBoolean(v)
      case JsArray(elements) => json.writeObject(elements)
      case JsObject(values) => {
        json.writeStartObject()
        values.foreach { t =>
          json.writeFieldName(t._1)
          json.writeObject(t._2)
        }
        json.writeEndObject()
      }
      case JsNull => json.writeNull()
      case JsUndefined(error) => {
        play.Logger.warn("Serializing an object with an undefined property: " + error)
        json.writeNull()
      }
    }
  }
}

@JsonCachable
class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): Object = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    val value = jp.getCurrentToken match {
      case JsonToken.VALUE_NUMBER_INT => JsNumber(jp.getDoubleValue)
      case JsonToken.VALUE_NUMBER_FLOAT => JsNumber(jp.getDoubleValue)
      case JsonToken.VALUE_STRING => JsString(jp.getText)
      case JsonToken.VALUE_TRUE => JsBoolean(true)
      case JsonToken.VALUE_FALSE => JsBoolean(false)
      case JsonToken.START_ARRAY => {
        JsArray(jp.getCodec.readValue(jp, Types.build(factory, manifest[List[JsValue]])))
      }
      case JsonToken.START_OBJECT => {
        jp.nextToken()
        deserialize(jp, ctxt)
      }
      case JsonToken.FIELD_NAME | JsonToken.END_OBJECT => {
        var fields = Map[String, JsValue]()
        while (jp.getCurrentToken != JsonToken.END_OBJECT) {
          val name = jp.getCurrentName
          jp.nextToken()
          fields = fields + (name -> jp.getCodec.readValue(jp, Types.build(factory, manifest[JsValue])))
          jp.nextToken()
        }
        JsObject(fields)
      }
      case _ => throw ctxt.mappingException(classOf[JsValue])
    }

    if (!klass.isAssignableFrom(value.getClass)) {
      throw ctxt.mappingException(klass)
    }

    value
  }
}

class PlayDeserializers(classLoader: ClassLoader) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig,
    provider: DeserializerProvider, beanDesc: BeanDescription,
    property: BeanProperty) = {
    val klass = javaType.getRawClass
    if (classOf[JsValue].isAssignableFrom(klass) || klass == JsNull.getClass) {
      new JsValueDeserializer(config.getTypeFactory, klass)
    } else null
  }

}

class PlaySerializers extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription, beanProp: BeanProperty) = {
    val ser: Object = if (classOf[JsValue].isAssignableFrom(beanDesc.getBeanClass)) {
      new JsValueSerializer
    } else {
      null
    }
    ser.asInstanceOf[JsonSerializer[Object]]
  }
}

private object Types {
  import java.util.concurrent.ConcurrentHashMap

  private val cachedTypes = scala.collection.JavaConversions.asScalaConcurrentMap(new ConcurrentHashMap[Manifest[_], JavaType]())

  def build(factory: TypeFactory, manifest: Manifest[_]): JavaType =
    cachedTypes.getOrElseUpdate(manifest, constructType(factory, manifest))

  private def constructType(factory: TypeFactory, manifest: Manifest[_]): JavaType = {
    if (manifest.erasure.isArray) {
      ArrayType.construct(factory.constructType(manifest.erasure.getComponentType), null, null)
    } else {
      factory.constructParametricType(
        manifest.erasure,
        manifest.typeArguments.map { m => build(factory, m) }.toArray: _*)
    }
  }
}

