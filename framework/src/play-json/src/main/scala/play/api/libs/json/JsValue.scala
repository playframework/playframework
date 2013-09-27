package play.api.libs.json

import com.fasterxml.jackson.core.{ JsonGenerator, JsonToken, JsonParser }
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.deser.Deserializers
import com.fasterxml.jackson.databind.ser.Serializers

import scala.collection._
import scala.collection.mutable.ListBuffer

import scala.annotation.tailrec
import play.api.data.validation.ValidationError

case class JsResultException(errors: Seq[(JsPath, Seq[ValidationError])]) extends RuntimeException("JsResultException(errors:%s)".format(errors))

/**
 * Generic json value
 */
sealed trait JsValue {

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
   * @return the resulting JsValue. If the current node is not a JsArray or the index is out of bounds, a JsUndefined will be returned.
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
   * Any error is mapped to None
   *
   * @return Some[T] if it succeeds, None if it fails.
   */
  def asOpt[T](implicit fjs: Reads[T]): Option[T] = fjs.reads(this).fold(
    invalid = _ => None,
    valid = v => Some(v)
  ).filter {
      case JsUndefined() => false
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
  def validate[T](implicit rds: Reads[T]): JsResult[T] = rds.reads(this)

  /**
   * Transforms a JsValue into another JsValue using provided Json transformer Reads[JsValue]
   */
  def transform[A <: JsValue](rds: Reads[A]): JsResult[A] = rds.reads(this)

  override def toString = Json.stringify(this)

  /**
   * Prune the Json AST according to the provided JsPath
   */
  //def prune(path: JsPath): JsValue = path.prune(this)

}

/**
 * Represent a Json null value.
 * with Scala 2.10-M7, this code generates WARNING : https://issues.scala-lang.org/browse/SI-6513
 */
case object JsNull extends JsValue

/**
 * Represent a missing Json value.
 */
class JsUndefined(err: => String) extends JsValue {
  def error = err
  override def toString = "JsUndefined(" + err + ")"
}

object JsUndefined {
  def apply(err: => String) = new JsUndefined(err)
  def unapply(o: Object): Boolean = o.isInstanceOf[JsUndefined]
}

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
 * Represent a Json array value.
 */
case class JsArray(value: Seq[JsValue] = List()) extends JsValue {

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

  def fieldSet: Set[(String, JsValue)] = fields.toSet

  /**
   * Merge this object with an other one. Values from other override value of the current object.
   */
  def ++(other: JsObject): JsObject =
    JsObject(fields.filterNot(field => other.keys(field._1)) ++ other.fields)

  /**
   * removes one field from JsObject
   */
  def -(otherField: String): JsObject =
    JsObject(fields.filterNot(_._1 == otherField))

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
            case _ => fields.foldLeft(List[(String, JsValue)]()) { (acc, field) =>
              field match {
                case (key, obj: JsObject) if (key == sv._1) =>
                  found = true
                  acc :+ key -> {
                    sv._2 match {
                      case o @ JsObject(_) => obj.deepMerge(o)
                      case js => js
                    }
                  }
                case (key, value) if (key == sv._1) =>
                  found = true
                  acc :+ key -> sv._2
                case (key, value) => acc :+ key -> value
              }
            }
          }

          if (!found) fields :+ sv
          else newFields

        case head :: tail =>
          var found = false
          val headFields = fields match {
            case List() => List(head)
            case _ => fields.foldLeft(List[(String, JsValue)]()) { (acc, field) =>
              field match {
                case (key, obj: JsObject) if (key == head._1) =>
                  found = true
                  acc :+ key -> {
                    head._2 match {
                      case o @ JsObject(_) => obj.deepMerge(o)
                      case js => js
                    }
                  }
                case (key, value) if (key == head._1) =>
                  found = true
                  acc :+ key -> head._2
                case (key, value) => acc :+ key -> value
              }
            }
          }

          if (!found) step(fields :+ head, tail)
          else step(headFields, tail)

      }
    }

    JsObject(step(fields.toList, other.fields.toList))
  }

  override def equals(other: Any): Boolean =
    other match {

      case that: JsObject =>
        (that canEqual this) &&
          fieldSet == that.fieldSet

      case _ => false
    }

  def canEqual(other: Any): Boolean = other.isInstanceOf[JsObject]

  override def hashCode: Int = fieldSet.hashCode()

}

// -- Serializers.

private[json] class JsValueSerializer extends JsonSerializer[JsValue] {

  override def serialize(value: JsValue, json: JsonGenerator, provider: SerializerProvider) {
    value match {
      case JsNumber(v) => json.writeNumber(v.bigDecimal)
      case JsString(v) => json.writeString(v)
      case JsBoolean(v) => json.writeBoolean(v)
      case JsArray(elements) => {
        json.writeStartArray()
        elements.foreach { t =>
          serialize(t, json, provider)
        }
        json.writeEndArray()
      }
      case JsObject(values) => {
        json.writeStartObject()
        values.foreach { t =>
          json.writeFieldName(t._1)
          serialize(t._2, json, provider)
        }
        json.writeEndObject()
      }
      case JsNull => json.writeNull()
      case JsUndefined() => {
        json.writeNull()
      }
    }
  }
}

private[json] sealed trait DeserializerContext {
  def addValue(value: JsValue): DeserializerContext
}

private[json] case class ReadingList(content: ListBuffer[JsValue]) extends DeserializerContext {
  override def addValue(value: JsValue): DeserializerContext = {
    ReadingList(content += value)
  }
}

// Context for reading an Object
private[json] case class KeyRead(content: ListBuffer[(String, JsValue)], fieldName: String) extends DeserializerContext {
  def addValue(value: JsValue): DeserializerContext = ReadingMap(content += (fieldName -> value))
}

// Context for reading one item of an Object (we already red fieldName)
private[json] case class ReadingMap(content: ListBuffer[(String, JsValue)]) extends DeserializerContext {

  def setField(fieldName: String) = KeyRead(content, fieldName)
  def addValue(value: JsValue): DeserializerContext = throw new Exception("Cannot add a value on an object without a key, malformed JSON object!")

}

private[json] class JsValueDeserializer(factory: TypeFactory, klass: Class[_]) extends JsonDeserializer[Object] {

  override def isCachable: Boolean = true

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsValue = {
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

      case (JsonToken.VALUE_NUMBER_INT | JsonToken.VALUE_NUMBER_FLOAT, c) => (Some(JsNumber(jp.getDecimalValue)), c)

      case (JsonToken.VALUE_STRING, c) => (Some(JsString(jp.getText)), c)

      case (JsonToken.VALUE_TRUE, c) => (Some(JsBoolean(true)), c)

      case (JsonToken.VALUE_FALSE, c) => (Some(JsBoolean(false)), c)

      case (JsonToken.VALUE_NULL, c) => (Some(JsNull), c)

      case (JsonToken.START_ARRAY, c) => (None, (ReadingList(ListBuffer())) +: c)

      case (JsonToken.END_ARRAY, ReadingList(content) :: stack) => (Some(JsArray(content)), stack)

      case (JsonToken.END_ARRAY, _) => throw new RuntimeException("We should have been reading list, something got wrong")

      case (JsonToken.START_OBJECT, c) => (None, ReadingMap(ListBuffer()) +: c)

      case (JsonToken.FIELD_NAME, (c: ReadingMap) :: stack) => (None, c.setField(jp.getCurrentName) +: stack)

      case (JsonToken.FIELD_NAME, _) => throw new RuntimeException("We should be reading map, something got wrong")

      case (JsonToken.END_OBJECT, ReadingMap(content) :: stack) => (Some(JsObject(content)), stack)

      case (JsonToken.END_OBJECT, _) => throw new RuntimeException("We should have been reading an object, something got wrong")

      case (JsonToken.NOT_AVAILABLE, _) => throw new RuntimeException("We should have been reading an object, something got wrong")

      case (JsonToken.VALUE_EMBEDDED_OBJECT, _) => throw new RuntimeException("We should have been reading an object, something got wrong")
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

  // This is used when the root object is null, ie when deserialising "null"
  override def getNullValue = JsNull
}

private[json] class PlayDeserializers(classLoader: ClassLoader) extends Deserializers.Base {
  override def findBeanDeserializer(javaType: JavaType, config: DeserializationConfig, beanDesc: BeanDescription) = {
    val klass = javaType.getRawClass
    if (classOf[JsValue].isAssignableFrom(klass) || klass == JsNull.getClass) {
      new JsValueDeserializer(config.getTypeFactory, klass)
    } else null
  }

}

private[json] class PlaySerializers extends Serializers.Base {
  override def findSerializer(config: SerializationConfig, javaType: JavaType, beanDesc: BeanDescription) = {
    val ser: Object = if (classOf[JsValue].isAssignableFrom(beanDesc.getBeanClass)) {
      new JsValueSerializer
    } else {
      null
    }
    ser.asInstanceOf[JsonSerializer[Object]]
  }
}

private[json] object JacksonJson {

  import com.fasterxml.jackson.core.Version
  import com.fasterxml.jackson.databind.module.SimpleModule
  import com.fasterxml.jackson.databind.Module.SetupContext

  private[this] val classLoader = Thread.currentThread().getContextClassLoader

  private[this] val mapper = (new ObjectMapper).registerModule(module)

  object module extends SimpleModule("PlayJson", Version.unknownVersion()) {
    override def setupModule(context: SetupContext) {
      context.addDeserializers(new PlayDeserializers(classLoader))
      context.addSerializers(new PlaySerializers)
    }
  }

  private[this] lazy val jsonFactory = new com.fasterxml.jackson.core.JsonFactory(mapper)

  private[this] def stringJsonGenerator(out: java.io.StringWriter) = jsonFactory.createGenerator(out)

  private[this] def jsonParser(c: String): JsonParser = jsonFactory.createParser(c)

  private[this] def jsonParser(data: Array[Byte]): JsonParser = jsonFactory.createParser(data)

  def parseJsValue(data: Array[Byte]): JsValue = {
    mapper.readValue(jsonParser(data), classOf[JsValue])
  }

  def parseJsValue(input: String): JsValue = {
    mapper.readValue(jsonParser(input), classOf[JsValue])
  }

  def generateFromJsValue(jsValue: JsValue): String = {
    val sw = new java.io.StringWriter
    val gen = stringJsonGenerator(sw)
    mapper.writeValue(gen, jsValue)
    sw.flush()
    sw.getBuffer.toString
  }

  def prettyPrint(jsValue: JsValue): String = {
    val sw = new java.io.StringWriter
    val gen = stringJsonGenerator(sw).setPrettyPrinter(
      new com.fasterxml.jackson.core.util.DefaultPrettyPrinter()
    )
    mapper.writerWithDefaultPrettyPrinter().writeValue(gen, jsValue)
    sw.flush()
    sw.getBuffer.toString
  }

}
