package play.api.json

import play.api.Json._

object AST {

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

}

