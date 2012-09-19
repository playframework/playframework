package play.api.libs.json

import org.codehaus.jackson.{ JsonGenerator, JsonToken, JsonParser }
import org.codehaus.jackson.`type`.JavaType
import org.codehaus.jackson.map._
import org.codehaus.jackson.map.annotate.JsonCachable
import org.codehaus.jackson.map.`type`.{ TypeFactory, ArrayType }

import scala.collection._

import scala.collection.immutable.Stack
import scala.annotation.tailrec
import play.api.data.validation.ValidationError


case class JsResultException(errors: Seq[(JsPath, Seq[ValidationError])]) extends RuntimeException( "JsResultException(errors:%s)".format(errors) )

/**
 * Generic json value
 */
sealed trait JsValue {
  import scala.util.control.Exception._

  /**
   * Return the property corresponding to the fieldName, supposing we have a JsObject.
   *
   * @param fieldName the name of the property to lookup
   * @return the resulting JsValue. If the current node is not a JsObject or doesn't have the property, a JsUndefined will be returned.
   */
  def \(fieldName: String): JsValue = JsUndefined("'" + fieldName + "'" + " is undefined on object: " + this)

  /**
   * Return the element at a given index, supposing we have a JsArray.
   *
   * @param idx the index to lookup
   * @param the resulting JsValue. If the current node is not a JsArray or the index is out of bounds, a JsUndefined will be returned.
   */
  def apply(idx: Int): JsValue = JsUndefined(this.toString + " is not an array")

  /**
   * Lookup for fieldName in the current object and all descendants.
   *
   * @return the list of matching nodes
   */
  def \\(fieldName: String): Seq[JsValue] = Nil

  /**
   * Tries to convert the node into a T. An implicit Reads[T] must be defined.
   *
   * @return Some[T] if it succeeds, None if it fails.
   */
  def asOpt[T](implicit fjs: Reads[T]): Option[T] = fjs.reads(this).fold(
      valid = v => Some(v),
      invalid = _ => None
    ).filter {
    case JsUndefined(_) => false
    case _ => true
  }

  /**
   * Tries to convert the node into a T, throwing an exception if it can't. An implicit Reads[T] must be defined.
   */
  def as[T](implicit fjs: Reads[T]): T = fjs.reads(this).fold(
    valid = identity,
    invalid = e => throw new JsResultException(e)
  )

  /**
   * Tries to convert the node into a JsResult[T] (Success or Error). An implicit Reads[T] must be defined.
   */
  def validate[T](implicit _reads: Reads[T]): JsResult[T] = _reads.reads(this)

  /**
   * Transforms a JsValue into another JsValue using given Writes[JsValue]
   */
  def transform(implicit _writes: Writes[JsValue]): JsValue = _writes.writes(this)

  override def toString = Json.stringify(this)

  /**
   * Prune the Json AST according to the provided JsPath
   */
  //def prune(path: JsPath): JsValue = path.prune(this)

}

/**
 * Represent a Json null value.
 */
case object JsNull extends JsValue

/**
 * Represent a missing Json value.
 */
case class JsUndefined(error: String) extends JsValue

/**
 * Represent a Json boolean value.
 */
case class JsBoolean(value: Boolean) extends JsValue

/**
 * Represent a Json number value.
 */
case class JsNumber(value: BigDecimal) extends JsValue

/**
 * Represent a Json string value.
 */
case class JsString(value: String) extends JsValue

/**
 * Represent a Json arayy value.
 */
case class JsArray(value: Seq[JsValue] = List()) extends JsValue{

  /**
   * Access a value of this array.
   *
   * @param index Element index.
   */
  override def apply(index: Int): JsValue = {
    value.lift(index).getOrElse(JsUndefined("Array index out of bounds in " + this))
  }

  /**
   * Lookup for fieldName in the current object and all descendants.
   *
   * @return the list of matching nodes
   */
  override def \\(fieldName: String): Seq[JsValue] = value.flatMap(_ \\ fieldName)

  /**
   * Concatenates this array with the elements of an other array.
   */
  def ++(other: JsArray): JsArray =
    JsArray(value ++ other.value)

  /**
   * Append an element to this array.
   */
  def :+(el: JsValue): JsArray = JsArray(value :+ el)
  def append(el: JsValue): JsArray = this.:+(el)

  /**
   * Prepend an element to this array.
   */
  def +:(el: JsValue): JsArray = JsArray(el +: value)
  def prepend(el: JsValue): JsArray = this.+:(el)

}

/**
 * Represent a Json object value.
 */
case class JsObject(fields: Seq[(String, JsValue)]) extends JsValue {

  lazy val value: Map[String, JsValue] = fields.toMap

  /**
   * Return the property corresponding to the fieldName, supposing we have a JsObject.
   *
   * @param fieldName the name of the property to lookup
   * @return the resulting JsValue. If the current node is not a JsObject or doesn't have the property, a JsUndefined will be returned.
   */
  override def \(fieldName: String): JsValue = value.get(fieldName).getOrElse(super.\(fieldName))

  /**
   * Lookup for fieldName in the current object and all descendants.
   *
   * @return the list of matching nodes
   */
  override def \\(fieldName: String): Seq[JsValue] = {
    value.foldLeft(Seq[JsValue]())((o, pair) => pair match {
      case (key, value) if key == fieldName => o ++ (value +: (value \\ fieldName))
      case (_, value) => o ++ (value \\ fieldName)
    })
  }

  /**
   * Return all keys
   */
  def keys: Set[String] = fields.map(_._1).toSet

  /**
   * Return all values
   */
  def values: Set[JsValue] = fields.map(_._2).toSet

  /**
   * Merge this object with an other one. Values from other override value of the current object.
   */
  def ++(other: JsObject): JsObject =
    JsObject(fields.filterNot(field => other.keys(field._1)) ++ other.fields)

  /**
   * removes one field from JsObject
   */
  def -(otherField: String): JsObject =
    JsObject(fields.filterNot( _._1 == otherField ))

  /**
   * adds one field from JsObject
   */
  def +(otherField: (String, JsValue)): JsObject =
    JsObject(fields :+ otherField)  

  /**
   * merges everything in depth and doesn't stop at first level as ++
   * TODO : improve because coding is nasty there
   */
  def deepMerge(other: JsObject): JsObject = {
    def step(fields: List[(String, JsValue)], others: List[(String, JsValue)]): Seq[(String, JsValue)] = {
      others match {
        case List() => fields
        case List(sv) => 
          var found = false
          val newFields = fields match {
            case List() => List(sv)
            case _ => fields.foldLeft(List[(String, JsValue)]()){ (acc, field) => field match {
              case (key, obj: JsObject) if(key == sv._1) => 
                found = true
                acc :+ key -> {
                  sv._2 match {
                    case o @ JsObject(_) => obj.deepMerge(o) 
                    case js => js
                  }
              }
              case (key, value) if(key == sv._1) => 
                found = true
                acc :+ key -> sv._2
              case (key, value) => acc :+ key -> value
            } }
          }
          
          if(!found) fields :+ sv
          else newFields

        case head :: tail => 
          var found = false
          val headFields = fields match {
            case List() => List(head)
              case _ => fields.foldLeft(List[(String, JsValue)]()){ (acc, field) => field match {
              case (key, obj: JsObject) if(key == head._1) => 
                found = true
                acc :+ key -> {
                  head._2 match {
                    case o @ JsObject(_) => obj.deepMerge(o) 
                    case js => js
                  }
                }
              case (key, value) if(key == head._1) => 
                found = true
                acc :+ key -> head._2
              case (key, value) => acc :+ key -> value
            } }
          }

          if(!found) step(fields :+ head, tail)
          else step(headFields, tail)
          
      }
    }

    JsObject(step(fields.toList, other.fields.toList))
  }

}

// -- Serializers.

@JsonCachable
private[json] class JsValueSerializer extends JsonSerializer[JsValue] {

  def serialize(value: JsValue, json: JsonGenerator, provider: SerializerProvider) {
    value match {
      case JsNumber(v) => json.writeNumber(v.bigDecimal)
      case JsString(v) => json.writeString(v)
      case JsBoolean(v) => json.writeBoolean(v)
      case JsArray(elements) => {
        json.writeStartArray()
        elements.foreach { t =>
          json.writeObject(t)
        }
        json.writeEndArray()
      }
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
        json.writeNull()
      }
    }
  }
}

private[json] sealed trait DeserializerContext {
  def addValue(value: JsValue): DeserializerContext
}

private[json] case class ReadingList(content: List[JsValue]) extends DeserializerContext {
  override def addValue(value: JsValue): DeserializerContext = {
    ReadingList(content :+ value)
  }
}

// Context for reading an Object
private[json] case class KeyRead(content: List[(String, JsValue)], fieldName: String) extends DeserializerContext {
  def addValue(value: JsValue): DeserializerContext = ReadingMap(content :+ (fieldName -> value))
}

// Context for reading one item of an Object (we already red fieldName)
private[json] case class ReadingMap(content: List[(String, JsValue)]) extends DeserializerContext {

  def setField(fieldName: String) = KeyRead(content, fieldName)
  def addValue(value: JsValue): DeserializerContext = throw new Exception("Cannot add a value on an object without a key, malformed JSON object!")

}

@JsonCachable
private[json] class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {
  def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsValue = {
    val value = deserialize(jp, ctxt, List())

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

    val (maybeValue, nextContext) = (jp.getCurrentToken, parserContext) match {

      case (JsonToken.VALUE_NUMBER_INT, c) => (Some(JsNumber(jp.getLongValue)), c)

      case (JsonToken.VALUE_NUMBER_FLOAT, c) => (Some(JsNumber(jp.getDoubleValue)), c)

      case (JsonToken.VALUE_STRING, c) => (Some(JsString(jp.getText)), c)

      case (JsonToken.VALUE_TRUE, c) => (Some(JsBoolean(true)), c)

      case (JsonToken.VALUE_FALSE, c) => (Some(JsBoolean(false)), c)

      case (JsonToken.VALUE_NULL, c) => (Some(JsNull), c)

      case (JsonToken.START_ARRAY, c) => (None, (ReadingList(List())) +: c)

      case (JsonToken.END_ARRAY, ReadingList(content) :: stack) => (Some(JsArray(content)), stack)

      case (JsonToken.END_ARRAY, _) => throw new RuntimeException("We should have been reading list, something got wrong")

      case (JsonToken.START_OBJECT, c) => (None, ReadingMap(List()) +: c)

      case (JsonToken.FIELD_NAME, (c: ReadingMap) :: stack) => (None, c.setField(jp.getCurrentName) +: stack)

      case (JsonToken.FIELD_NAME, _) => throw new RuntimeException("We should be reading map, something got wrong")

      case (JsonToken.END_OBJECT, ReadingMap(content) :: stack) => (Some(JsObject(content)), stack)

      case (JsonToken.END_OBJECT, _) => throw new RuntimeException("We should have been reading an object, something got wrong")

      case _ => throw ctxt.mappingException(classOf[JsValue])
    }

    // Read ahead
    jp.nextToken()

    maybeValue match {
      case Some(v) if nextContext.isEmpty && jp.getCurrentToken == null =>
        //done, no more tokens and got a value!
        v

      case Some(v) if nextContext.isEmpty =>
        //strange, got value, but there is more tokens and have no prior context!
        throw new Exception("Malformed JSON: Got a sequence of JsValue outside an array or an object.")

      case maybeValue =>
        val toPass = maybeValue.map { v =>
          val previous :: stack = nextContext
          (previous.addValue(v)) +: stack
        }.getOrElse(nextContext)

        deserialize(jp, ctxt, toPass)

    }

  }
}

private[json] class PlayDeserializers(classLoader: ClassLoader) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig,
    provider: DeserializerProvider, beanDesc: BeanDescription,
    property: BeanProperty) = {
    val klass = javaType.getRawClass
    if (classOf[JsValue].isAssignableFrom(klass) || klass == JsNull.getClass) {
      new JsValueDeserializer(config.getTypeFactory, klass)
    } else null
  }

}

private[json] class PlaySerializers extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription, beanProp: BeanProperty) = {
    val ser: Object = if (classOf[JsValue].isAssignableFrom(beanDesc.getBeanClass)) {
      new JsValueSerializer
    } else {
      null
    }
    ser.asInstanceOf[JsonSerializer[Object]]
  }
}

private[json] object JacksonJson{

  import org.codehaus.jackson.Version
  import org.codehaus.jackson.map.module.SimpleModule
  import org.codehaus.jackson.map.Module.SetupContext

  private[this]  val classLoader = Thread.currentThread().getContextClassLoader

  private[this] val mapper = new ObjectMapper
  
  object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
    override def setupModule(context: SetupContext) {
      context.addDeserializers(new PlayDeserializers(classLoader))
      context.addSerializers(new PlaySerializers)
    }
  }
  mapper.registerModule(module)

  private[this] lazy val jsonFactory = new org.codehaus.jackson.JsonFactory(mapper)

  private[this] def stringJsonGenerator(out: java.io.StringWriter) = jsonFactory.createJsonGenerator(out)
  
  private[this] def jsonParser(c: String) = jsonFactory.createJsonParser(c)

  

  def parseJsValue(input: String): JsValue = {
    mapper.readValue(jsonParser(input), classOf[JsValue])
  }

  def generateFromJsValue(jsValue: JsValue): String = {
    val sw = new java.io.StringWriter
    val gen = stringJsonGenerator(sw)
    mapper.writeValue(gen, jsValue)
    sw.flush
    sw.getBuffer.toString
  }

 

}
