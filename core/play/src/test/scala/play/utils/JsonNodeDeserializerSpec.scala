/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

import java.math.MathContext

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import org.specs2.mutable.Specification

class JsonNodeDeserializerSpec extends BaseJacksonDeserializer("JsonNodeDeserializer") {
  def baseMapper(): ObjectMapper =
    JsonMapper.builder
      .build()
      .registerModule(JacksonJsonNodeModule)

  override def baseMapper(jsonReadFeature: JsonReadFeature): ObjectMapper =
    JsonMapper.builder
      .enable(jsonReadFeature)
      .build()
      .registerModule(JacksonJsonNodeModule)
}

class DefaultDeserializerSpec extends BaseJacksonDeserializer("default") {
  def baseMapper(): ObjectMapper = JsonMapper.builder.build()

  override def baseMapper(jsonReadFeature: JsonReadFeature): ObjectMapper =
    JsonMapper.builder
      .enable(jsonReadFeature)
      .build()
}

abstract class BaseJacksonDeserializer(val implementationName: String) extends Specification {

  val jsonStringPrimitives: String =
    s"""
       | { 
       |   "intValue" : 23 , 
       |   "longValue" : ${Long.MaxValue},
       |   "floatValue" : 3.141592,
       |   "doubleValue" : ${Double.MaxValue}
       | } 
       |""".stripMargin

  val jsonStringBigNums: String =
    s"""
       | { 
       |   "intValue" : 23 , 
       |   "floatValue" : 3.141592
       | } 
       |""".stripMargin

  def baseMapper(): ObjectMapper
  def baseMapper(jsonReadFeature: JsonReadFeature): ObjectMapper

  s"A $implementationName Jackson ObjectMapper" >> {

    "parse numbers as primitives" >> {
      val mapper: ObjectMapper = baseMapper()
      mapper.readValue(jsonStringPrimitives, classOf[PrimitiveNumericJavaPojo]) must equalTo(
        new PrimitiveNumericJavaPojo(
          23,
          Long.MaxValue,
          3.141592f,
          Double.MaxValue
        )
      )
    }

    "parse numbers as BigDecimal/Integer (implicitly)" >> {
      val mapper: ObjectMapper = baseMapper()
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, false)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, false)
      mapper.readValue(jsonStringPrimitives, classOf[PrimitiveNumericJavaPojo]) must equalTo(
        new PrimitiveNumericJavaPojo(
          23,
          Long.MaxValue,
          3.141592f,
          Double.MaxValue
        )
      )
      mapper.readValue(jsonStringBigNums, classOf[BigNumericJavaPojo]) must equalTo(
        new BigNumericJavaPojo(
          java.math.BigInteger.valueOf(23),
          java.math.BigDecimal.valueOf(3141592, 6)
        )
      )
    }

    "parse numbers as BigDecimal/Integer (explicitly)" >> {
      val mapper: ObjectMapper = baseMapper(JsonReadFeature.ALLOW_MISSING_VALUES)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
      mapper.readValue(jsonStringBigNums, classOf[BigNumericJavaPojo]) must equalTo(
        new BigNumericJavaPojo(
          java.math.BigInteger.valueOf(23),
          java.math.BigDecimal.valueOf(3141592, 6)
        )
      )

    }

    def readNode(mapper: ObjectMapper, json: String) =
      mapper.readTree(json).findValue("value")

    "read Float" >> {
      // in both Jackson and in our own impl, Floats become a DoubleNode at tree level
      val json = """{ "value" : 0.1 }"""
      readNode(baseMapper(), json).isDouble must beTrue
    }

    "read Double" >> {
      val json = """{ "value" : 1.7976931348623157E308 }"""
      readNode(baseMapper(), json).isDouble must beTrue
    }

    "read Double as BigDecimal" >> {
      val json = """{ "value" : 1.7976931348623157E308 }"""

      val mapper = baseMapper().configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      readNode(mapper, json).isBigDecimal must beTrue
    }

    "read NaN as Double" >> {
      val json = """{ "value" : NaN }"""
      val mapper =
        baseMapper(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
          .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)

      readNode(mapper, json).isDouble must beTrue
    }

    "read Int" >> {
      val json = """{ "value" : 10 }"""
      readNode(baseMapper(), json).isInt must beTrue
    }

    "read Int as Long" >> {
      val json   = """{ "value" : 10 }"""
      val mapper = baseMapper().configure(DeserializationFeature.USE_LONG_FOR_INTS, true)
      readNode(mapper, json).isLong must beTrue
    }

    "read Int as BigInteger" >> {
      val json   = """{ "value" : 10 }"""
      val mapper = baseMapper().configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
      readNode(mapper, json).isBigInteger must beTrue
    }

    "read Long" >> {
      val json = s"""{ "value" : ${Long.MaxValue} }"""
      readNode(baseMapper(), json).isLong must beTrue
    }

    "read BigInteger" >> {
      val json = s"""{ "value" : ${Long.MaxValue}0000 }"""
      readNode(baseMapper(), json).isBigInteger must beTrue
    }
  }

}
