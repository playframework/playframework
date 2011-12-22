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

case object JsNull extends JsValue

case class JsUndefined(error: String) extends JsValue

case class JsBoolean(value: Boolean) extends JsValue

case class JsNumber(value: BigDecimal) extends JsValue

case class JsString(value: String) extends JsValue

case class JsArray(value: List[JsValue]) extends JsValue {

  override def apply(index: Int): JsValue = 
    value.lift(index).getOrElse(JsUndefined("Array index out of bounds in " + this))

  override def \\(fieldName: String): Seq[JsValue] = value.flatMap(_ \\ fieldName)

}

case class JsObject(value: Map[String, JsValue]) extends JsValue {

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

sealed trait DeserializerContext{
  def addValue(value: JsValue): DeserializerContext
}

case class ReadingList(content: List[JsValue]) extends DeserializerContext {
  override def addValue(value: JsValue): DeserializerContext = {
   ReadingList(content :+ value) 
  }
}

// Context for reading an Object
case class KeyRead(content: Map[String, JsValue], fieldName:String) extends DeserializerContext {
  def addValue(value: JsValue): DeserializerContext = ReadingMap(content + (fieldName -> value))
}

// Context for reading one item of an Object (we already red fieldName)
case class ReadingMap(content: Map[String, JsValue]) extends DeserializerContext {

  def setField(fieldName:String) = KeyRead(content, fieldName)
  def addValue(value: JsValue): DeserializerContext = throw new Exception("Cannot add a value on an object without a key, malformed JSON object!")

}

@JsonCachable
private class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsValue = {
    val value = deserialize(jp, ctxt,List())

    if (!klass.isAssignableFrom(value.getClass)) {
      throw ctxt.mappingException(klass)
    }
    value
  }

  @tailrec
  final def deserialize(jp: JsonParser, ctxt: DeserializationContext, parserContext: List[DeserializerContext]): JsValue = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    val (maybeValue,nextContext) = (jp.getCurrentToken, parserContext)  match {

      case (JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT, c) => (Some(JsNumber(jp.getDoubleValue)), c)

      case (JsonToken.VALUE_STRING, c) => (Some(JsString(jp.getText)),c)
        
      case (JsonToken.VALUE_TRUE, c) => (Some(JsBoolean(true)),c)

      case (JsonToken.VALUE_FALSE, c) => (Some(JsBoolean(false)),c)

      case (JsonToken.VALUE_NULL, c) => (Some(JsNull),c)

      case (JsonToken.START_ARRAY, c) => (None,(ReadingList(List())) +: c)

      case (JsonToken.END_ARRAY, ReadingList(content) :: stack) =>  (Some(JsArray(content)),stack)

      case (JsonToken.END_ARRAY,_) => throw new RuntimeException("We should have been reading list, something got wrong")

      case (JsonToken.START_OBJECT, c) => (None,ReadingMap(Map()) +: c )

      case (JsonToken.FIELD_NAME, (c: ReadingMap) :: stack) =>  (None, c.setField(jp.getCurrentName) +: stack )

      case (JsonToken.FIELD_NAME,_) => throw new RuntimeException("We should be reading map, something got wrong")

      case (JsonToken.END_OBJECT, ReadingMap(content) :: stack) =>  (Some(JsObject(content)),stack)

      case (JsonToken.END_OBJECT,_) => throw new RuntimeException("We should have been reading an object, something got wrong")
        
      case _ => throw ctxt.mappingException(classOf[JsValue])
    }

    // Read ahead
    jp.nextToken()

    maybeValue match { 
      case Some(v) if nextContext.isEmpty && jp.getCurrentToken == null =>
        //done, no more tokens and got a value!
        v 

      case Some(v) if nextContext.isEmpty  =>
        //strange, got value, but there is more tokens and have no prior context!
        throw new Exception("Malformed JSON: Got a sequence of JsValue outside an array or an object.")

      case maybeValue  => 
        val toPass = maybeValue.map{ v => 
          val previous :: stack = nextContext
          (previous.addValue(v)) +: stack
        }.getOrElse(nextContext)

        deserialize(jp, ctxt,toPass)

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
