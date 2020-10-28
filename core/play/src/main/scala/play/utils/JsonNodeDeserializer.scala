/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

import java.math.MathContext

import scala.annotation.switch
import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.ListBuffer

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonTokenId
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_LONG_FOR_INTS
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node._

private sealed trait DeserializerContext {
  def addValue(value: JsonNode): DeserializerContext
}

private case class ReadingList(content: mutable.ArrayBuffer[JsonNode]) extends DeserializerContext {
  override def addValue(value: JsonNode): DeserializerContext = {
    ReadingList(content += value)
  }
}

// Context for reading an Object
private case class KeyRead(content: ListBuffer[(String, JsonNode)], fieldName: String) extends DeserializerContext {
  def addValue(value: JsonNode): DeserializerContext = ReadingMap(content += (fieldName -> value))
}

// Context for reading one item of an Object (we already red fieldName)
private case class ReadingMap(content: ListBuffer[(String, JsonNode)]) extends DeserializerContext {
  def setField(fieldName: String) = KeyRead(content, fieldName)
  def addValue(value: JsonNode): DeserializerContext =
    throw new Exception("Cannot add a value on an object without a key, malformed JSON object!")
}

class JacksonJsonNodeModule extends SimpleModule {
  override def getModuleName = "JacksonJsonNodeModule"
  addDeserializer(classOf[JsonNode], new JsonNodeDeserializer())

}

object JacksonJsonNodeModule extends JacksonJsonNodeModule

private class JsonNodeDeserializer extends JsonDeserializer[JsonNode] {
  override def isCachable: Boolean = true

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): JsonNode =
    deserialize(jp, ctxt, Nil)

  //====================================================================================
  // NOTE: the following two methods are re-encoding part of
  // com.fasterxml.jackson.databind.deser.std.JsonNodeDeserializer
  private val F_MASK_INT_COERCIONS: Int =
    USE_BIG_INTEGER_FOR_INTS.getMask | USE_LONG_FOR_INTS.getMask

  /* re-encoding of part of JsonNodeDeserializer (jackson-databind 2.10.5)
   * https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.10.5/src/main/java/com/fasterxml/jackson/databind/deser/std/JsonNodeDeserializer.java#L556-L579
   * TODO: remove when jackson-databind 2.12 is out
   */
  private def fromInt(jp: JsonParser, ctxt: DeserializationContext): JsonNode = {
    val feats = ctxt.getDeserializationFeatures

    val nodeFactory = ctxt.getNodeFactory

    val numberType =
      if ((feats & F_MASK_INT_COERCIONS) != 0) {
        if (USE_BIG_INTEGER_FOR_INTS.enabledIn(feats)) JsonParser.NumberType.BIG_INTEGER
        else if (USE_LONG_FOR_INTS.enabledIn(feats)) JsonParser.NumberType.LONG
        else jp.getNumberType
      } else jp.getNumberType

    numberType match {
      case JsonParser.NumberType.INT  => nodeFactory.numberNode(jp.getIntValue)
      case JsonParser.NumberType.LONG => nodeFactory.numberNode(jp.getLongValue)
      case _                          => nodeFactory.numberNode(jp.getBigIntegerValue)
    }
  }

  /* re-encoding of part of JsonNodeDeserializer (jackson-databind 2.10.5)
   * https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.10.5/src/main/java/com/fasterxml/jackson/databind/deser/std/JsonNodeDeserializer.java#L581-L600
   * TODO: remove when jackson-databind 2.12 is out
   */
  private def fromFloat(jp: JsonParser, ctxt: DeserializationContext): JsonNode = {
    val nodeFactory = ctxt.getNodeFactory

    val nt = jp.getNumberType
    if (nt eq JsonParser.NumberType.BIG_DECIMAL) {
      nodeFactory.numberNode(jp.getDecimalValue)

    } else if (ctxt.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)) {
      if (jp.isNaN) nodeFactory.numberNode(jp.getDoubleValue)
      else nodeFactory.numberNode(jp.getDecimalValue)

    } else if (nt eq JsonParser.NumberType.FLOAT) {
      nodeFactory.numberNode(jp.getFloatValue)

    } else {
      nodeFactory.numberNode(jp.getDoubleValue)
    }

  }

  //====================================================================================

  /* re-encoding of part of JsonNodeDeserializer (jackson-databind 2.10.5)
   * https://github.com/FasterXML/jackson-databind/blob/jackson-databind-2.10.5/src/main/java/com/fasterxml/jackson/databind/deser/std/JsonNodeDeserializer.java#L602-L623
   * TODO: remove when jackson-databind 2.12 is out
   */

  private def fromEmbedded(p: JsonParser, ctxt: DeserializationContext): JsonNode = {
    val nodeFactory = ctxt.getNodeFactory
    import com.fasterxml.jackson.databind.JsonNode
    import com.fasterxml.jackson.databind.util.RawValue
    val ob = p.getEmbeddedObject

    // should this occur?
    if (ob == null) {
      nodeFactory.nullNode
    } else {
      val `type` = ob.getClass
      if (`type` eq classOf[Array[Byte]]) {
        // most common special case
        nodeFactory.binaryNode(ob.asInstanceOf[Array[Byte]])
      } else if (ob.isInstanceOf[RawValue]) {
        nodeFactory.rawValueNode(ob.asInstanceOf[RawValue])
      } else if (ob.isInstanceOf[JsonNode]) {
        ob.asInstanceOf[JsonNode]
      } else {
        nodeFactory.pojoNode(ob)
      }
    }
  }

  //====================================================================================

  @tailrec
  final def deserialize(
      jp: JsonParser,
      ctxt: DeserializationContext,
      parserContext: List[DeserializerContext]
  ): JsonNode = {
    if (jp.getCurrentToken == null) {
      jp.nextToken()
    }

    val (maybeValue, nextContext) = (jp.getCurrentToken.id(): @switch) match {
      case JsonTokenId.ID_NUMBER_INT   => (Some(fromInt(jp, ctxt)), parserContext)
      case JsonTokenId.ID_NUMBER_FLOAT => (Some(fromFloat(jp, ctxt)), parserContext)
      case JsonTokenId.ID_STRING       => (Some(new TextNode(jp.getText)), parserContext)
      case JsonTokenId.ID_TRUE         => (Some(BooleanNode.TRUE), parserContext)
      case JsonTokenId.ID_FALSE        => (Some(BooleanNode.FALSE), parserContext)
      case JsonTokenId.ID_NULL         => (Some(NullNode.instance), parserContext)
      case JsonTokenId.ID_START_ARRAY  => (None, ReadingList(ArrayBuffer()) +: parserContext)

      case JsonTokenId.ID_END_ARRAY =>
        parserContext match {
          case ReadingList(content) :: stack =>
            val node = new ArrayNode(ctxt.getNodeFactory)
            content.foreach(node.add)
            (Some(node), stack)
          case _ => throw new RuntimeException("We should have been reading list, something got wrong")
        }

      case JsonTokenId.ID_START_OBJECT => (None, ReadingMap(ListBuffer()) +: parserContext)

      case JsonTokenId.ID_FIELD_NAME =>
        parserContext match {
          case (c: ReadingMap) :: stack => (None, c.setField(jp.getCurrentName) +: stack)
          case _                        => throw new RuntimeException("We should be reading map, something got wrong")
        }

      case JsonTokenId.ID_END_OBJECT =>
        parserContext match {
          case ReadingMap(content) :: stack =>
            val node = new ObjectNode(ctxt.getNodeFactory)
            content.foreach {
              case (k, v: JsonNode) =>
                node.set[JsonNode](k, v)
            }
            (Some(node), stack)
          case _ => throw new RuntimeException("We should have been reading an object, something got wrong")
        }

      case JsonTokenId.ID_NOT_AVAILABLE =>
        throw new RuntimeException("Didn't receive a token when requesting one. See Jackson's JsonToken#NOT_AVAILABLE.")

      case JsonTokenId.ID_EMBEDDED_OBJECT => (Some(fromEmbedded(jp, ctxt)), parserContext)
    }

    // Read ahead
    jp.nextToken()

    maybeValue match {
      case Some(v) if nextContext.isEmpty =>
        // done, no more tokens and got a value!
        // note: jp.getCurrentToken == null happens when using treeToValue (we're not parsing tokens)
        v

      case maybeValue =>
        val toPass = maybeValue
          .map { v =>
            val previous :: stack = nextContext
            (previous.addValue(v)) +: stack
          }
          .getOrElse(nextContext)

        deserialize(jp, ctxt, toPass)
    }
  }

  // This is used when the root object is null, ie when deserializing "null"
  override val getNullValue = NullNode.instance
}
