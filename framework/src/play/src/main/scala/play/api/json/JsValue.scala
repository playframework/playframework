package play.api.json

import org.codehaus.jackson.{ JsonGenerator, JsonToken, JsonParser }
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import org.codehaus.jackson.map.annotate.JsonCachable
import org.codehaus.jackson.map.`type`.{ TypeFactory, ArrayType }

import scala.collection._

import scala.collection.immutable.Stack
import scala.annotation.tailrec


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

sealed trait DeserializerContext {
  def read(): JsValue = JsUndefined("Uninitialized context")

  def addValue(value: JsValue): DeserializerContext = throw new RuntimeException("Could not add value here")
}

case class DefaultBaseDeserializerContext extends DeserializerContext {
  override def addValue(value: JsValue): BaseDeserializerContext = BaseDeserializerContext(value)
}

case class BaseDeserializerContext(content: JsValue) extends DeserializerContext {
  override def read(): JsValue = content
}

case class ReadingListDeserializerContext(content: List[JsValue], fatherStack: DeserializerContext) extends DeserializerContext {
  override def addValue(value: JsValue): ReadingListDeserializerContext = {
    // We will make a new context base on HEAD (father) and content
    // of this context + value then return the brand new context

    // First get back HEAD
    val newFather = fatherStack

    // Create new content
    val newContent = content :+ value

    ReadingListDeserializerContext(newContent, newFather)
  }
}

// Context for reading an Object
case class ReadingMapDeserializerContext(content: Map[String, JsValue], fatherStack: DeserializerContext) extends DeserializerContext {
  def setField(fieldName: String): ReadingMapWithFieldDeserializerContext = ReadingMapWithFieldDeserializerContext(fieldName, this)
}

// Context for reading one item of an Object (we already red fieldName)
case class ReadingMapWithFieldDeserializerContext(fieldName: String, fatherStack: ReadingMapDeserializerContext) extends DeserializerContext {
  override def addValue(value: JsValue): ReadingMapDeserializerContext = {
    // We will make a new context base on HEAD^2 (father of father) 
    // and content of HEAD (father) + (fieldName -> value)
    // then return the brand new context

    // First get back HEAD^2
    val newFather = fatherStack.fatherStack

    // Get back current content of HEAD
    val currentContent = fatherStack.content

    // Create new content
    val newContent = currentContent + (fieldName -> value)

    // We're still reading map, but this time ... we don't have field yet
    ReadingMapDeserializerContext(newContent, newFather)
  }
}

@JsonCachable
private class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsValue = {
    val value = deserialize(jp, ctxt, DefaultBaseDeserializerContext())

    if (!klass.isAssignableFrom(value.getClass)) {
      throw ctxt.mappingException(klass)
    }
    value
  }

  @tailrec
  final def deserialize(jp: JsonParser, ctxt: DeserializationContext, parserContext: DeserializerContext = DefaultBaseDeserializerContext()): JsValue = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    jp.getCurrentToken match {
      case JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT=> {
        // Read Value
        val value = jp.getDoubleValue

        // Read ahead
        jp.nextToken()

        // Continue parsing
        deserialize(jp, ctxt, parserContext.addValue(JsNumber(value)))
      }
      case JsonToken.VALUE_STRING => {
        // Read value
        val value = jp.getText

        // Read ahead
        jp.nextToken()

        // Continue parsing
        deserialize(jp, ctxt, parserContext.addValue(JsString(value)))
        }
      case JsonToken.VALUE_TRUE => {
        jp.nextToken()
        deserialize(jp, ctxt, parserContext.addValue(JsBoolean(true)))
      }
      case JsonToken.VALUE_FALSE => {
        jp.nextToken()
        deserialize(jp, ctxt, parserContext.addValue(JsBoolean(false)))
      }
      case JsonToken.VALUE_NULL => {
        jp.nextToken()
        deserialize(jp, ctxt, parserContext.addValue(JsNull))
      }
      case JsonToken.START_ARRAY => {
        jp.nextToken()

        deserialize(jp, ctxt, ReadingListDeserializerContext(List[JsValue](), parserContext))
      }
      case JsonToken.END_ARRAY => {
        parserContext match {
          case ReadingListDeserializerContext(content, fatherStack) => {
            // Throw the current end array and read new token
            jp.nextToken()

            deserialize(jp, ctxt, fatherStack.addValue(JsArray(content)))
          }
          case _ => throw new RuntimeException("We should have been reading list, something got wrong")
        }
      }
      case JsonToken.START_OBJECT => {
        // Depile start_object
        jp.nextToken()

        // Bootstrap a new context
        deserialize(jp, ctxt, ReadingMapDeserializerContext(Map[String, JsValue](), parserContext))
      }
      case JsonToken.FIELD_NAME => {
        parserContext match {
          case c: ReadingMapDeserializerContext => {
            // Read field name
            val name = jp.getCurrentName

            // Content has been read, move ahead
            jp.nextToken()

            // Continue serialize
            deserialize(jp, ctxt, c.setField(name))
          }
          case _ => throw new RuntimeException("We should be reading map, something got wrong")
        }
      }
      case JsonToken.END_OBJECT => {
        parserContext match {
          case ReadingMapDeserializerContext(content, stack) => {
            // Read ahead
            jp.nextToken()

            // Continue serialize
            deserialize(jp, ctxt, stack.addValue(JsObject(content)))
          }
          case _ => throw new RuntimeException("We should have been reading an object, something got wrong")
        }
      }
      case null => parserContext.read
      case _ => throw ctxt.mappingException(classOf[JsValue])
    }
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
