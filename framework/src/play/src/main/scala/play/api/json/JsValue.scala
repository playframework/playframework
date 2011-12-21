package play.api.json

import org.codehaus.jackson.{ JsonGenerator, JsonToken, JsonParser }
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import org.codehaus.jackson.map.annotate.JsonCachable
import org.codehaus.jackson.map.`type`.{ TypeFactory, ArrayType }

import scala.collection._

import scala.collection.immutable.Stack

/**
 * Generic json value
 */
sealed trait JsValue {
  import scala.util.control.Exception._

  def value: Any

  def valueAs[A]: A = value.asInstanceOf[A]

  /**
   * Return the property corresponding to the fieldName, supposing we have a JsObject.
   * @param fieldName the name of the property to lookup
   * @return the resulting JsValue. If the current node is not a JsObject or doesn't have the property, a JsUndefined will be returned.
   */
  def \(fieldName: String): JsValue = JsUndefined("'" + fieldName + "'" + " is undefined on object: " + this)

  /**
   * Return the element at a given index, supposing we have a JsArray.
   * @param idx the index to lookup
   * @param the resulting JsValue. If the current node is not a JsArray or the index is out of bounds, a JsUndefined will be returned.
   */
  def apply(idx: Int): JsValue = JsUndefined(this.toString + " is not an array")

  /**
   * Lookup for fieldName in the current object and all descendants.
   * @return the list of matching nodes
   */
  def \\(fieldName: String): Seq[JsValue] = Nil

  /**
   * Tries to convert the node into a T. An implicit Reads[T] must be defined.
   * @return Some[T] if it succeeds, None if it fails.
   */
  def asOpt[T](implicit fjs: Reads[T]): Option[T] = catching(classOf[RuntimeException]).opt(fjs.reads(this))

  /**
   * Tries to convert the node into a T, throwing an exception if it can't. An implicit Reads[T] must be defined.
   */
  def as[T](implicit fjs: Reads[T]): T = fjs.reads(this)

  /**
   * tries to convert from [K,V] to Map[K,V]
   */
  def as[K, V](implicit fjs: Reads[collection.immutable.Map[K, V]]): collection.immutable.Map[K, V] = fjs.reads(this)

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
      case _ => JsUndefined("Array index out of bounds in " + this)
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

@JsonCachable
private class JsValueSerializer extends JsonSerializer[JsValue] {

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
private class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsValue = deserialize(jp, ctxt, Stack[JsValue]())

  def deserialize(jp: JsonParser, ctxt: DeserializationContext, parserContext: Stack[JsValue] = Stack[JsValue]()): JsValue = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    val value = jp.getCurrentToken match {
      case JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT=> {
        // Read Value
        val value = jp.getDoubleValue

        // Read ahead
        jp.nextToken()

        // Continue parsing
        deserialize(jp, ctxt, JsNumber(value) +: parserContext)
      }
      case JsonToken.VALUE_STRING => {
        // Read value
        val value = jp.getText

        // Read ahead
        jp.nextToken()

        // Continue parsing
        deserialize(jp, ctxt, JsString(value) +: parserContext)
        }
      case JsonToken.VALUE_TRUE => {
        jp.nextToken()
        deserialize(jp, ctxt, JsBoolean(true) +: parserContext)
      }
      case JsonToken.VALUE_FALSE => {
        jp.nextToken()
        deserialize(jp, ctxt, JsBoolean(false) +: parserContext)
      }
      case JsonToken.VALUE_NULL => {
        jp.nextToken()
        deserialize(jp, ctxt, JsNull +: parserContext)
      }
      case JsonToken.START_ARRAY => {
        jp.nextToken()

        // We use JsUndefined as a stop condition on stack pop
        deserialize(jp, ctxt, JsUndefined("") +: parserContext)
      }
      case JsonToken.END_ARRAY => {
        var stack = parserContext

        var elements = List[JsValue]()

        var stillArray = true

        while(stillArray) {
          val value = stack.pop2
          value match{
            // Stop condition reached
            case (JsUndefined(_), s) => {
              stillArray = false
              stack = stack.pop
              }
            case (v, s) => {
              elements = v +: elements 
              stack = s
              }
            }
          }

        // Throw old object and build a new one
        jp.nextToken()

        deserialize(jp, ctxt, JsArray(elements) +: stack)
}
      case JsonToken.START_OBJECT => {
        // Depile start_object
        jp.nextToken()

        // We use JsUndefined as a stop condition on stack pop
        deserialize(jp, ctxt, JsUndefined("") +: parserContext)
      }
      case JsonToken.FIELD_NAME => {
        // Read field name
        val name = jp.getCurrentName

        // Pop separator
        jp.nextToken()

        // Continue serialize
        deserialize(jp, ctxt, JsString(name) +: parserContext)
      }
      case JsonToken.END_OBJECT => { 
        var stack = parserContext

        var fields = Map[String, JsValue]()

        var stillObject = true

        while(stillObject) {
          val value = stack.pop2
          value match{
            // Stop condition reached
            case (JsUndefined(_), s) => {
              stillObject = false
              stack = stack.pop
              }
            case (v, s) => {
              s.pop2 match {
                case (JsString(k), t) => {
                  fields = fields + (k -> v)
                  stack = t
                  }
                }
              }
            }
          }

        // Throw old object and build a new one
        jp.nextToken()

        deserialize(jp, ctxt, JsObject(fields) +: stack)
        }
      case null => parserContext.last
      case _ => throw ctxt.mappingException(classOf[JsValue])
    }

    if (!klass.isAssignableFrom(value.getClass)) {
      throw ctxt.mappingException(klass)
    }

    value
  }
}

private class PlayDeserializers(classLoader: ClassLoader) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig,
    provider: DeserializerProvider, beanDesc: BeanDescription,
    property: BeanProperty) = {
    val klass = javaType.getRawClass
    if (classOf[JsValue].isAssignableFrom(klass) || klass == JsNull.getClass) {
      new JsValueDeserializer(config.getTypeFactory, klass)
    } else null
  }

}

private class PlaySerializers extends Serializers.Base {
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

private object JerksonJson extends com.codahale.jerkson.Json {
  import org.codehaus.jackson.Version
  import org.codehaus.jackson.map.module.SimpleModule
  import org.codehaus.jackson.map.Module.SetupContext

  object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
    override def setupModule(context: SetupContext) {
      context.addDeserializers(new PlayDeserializers(classLoader))
      context.addSerializers(new PlaySerializers)
    }
  }
  mapper.registerModule(module)

}
