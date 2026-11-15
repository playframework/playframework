/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.utils

import org.specs2.mutable.Specification
import tools.jackson.core.json.JsonReadFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper

class DefaultDeserializerSpec extends BaseJacksonDeserializer("default") {
  def baseMapper(): JsonMapper.Builder = JsonMapper.builder

  override def baseMapper(jsonReadFeature: JsonReadFeature): JsonMapper.Builder =
    JsonMapper.builder
      .enable(jsonReadFeature)
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

  def baseMapper(): JsonMapper.Builder
  def baseMapper(jsonReadFeature: JsonReadFeature): JsonMapper.Builder

  s"A $implementationName Jackson ObjectMapper" >> {

    "parse numbers as primitives" >> {
      val mapper: ObjectMapper = baseMapper().build()
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
        .build()
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
        .build()
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
      readNode(baseMapper().build(), json).isDouble must beTrue
    }

    "read Double" >> {
      val json = """{ "value" : 1.7976931348623157E308 }"""
      readNode(baseMapper().build(), json).isDouble must beTrue
    }

    "read Double as BigDecimal" >> {
      val json = """{ "value" : 1.7976931348623157E308 }"""

      val mapper = baseMapper().configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true).build()
      readNode(mapper, json).isBigDecimal must beTrue
    }

    "read NaN as Double" >> {
      val json   = """{ "value" : NaN }"""
      val mapper =
        baseMapper(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS)
          .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
          .build()

      readNode(mapper, json).isDouble must beTrue
    }

    "read Int" >> {
      val json = """{ "value" : 10 }"""
      readNode(baseMapper().build(), json).isInt must beTrue
    }

    "read Int as Long" >> {
      val json   = """{ "value" : 10 }"""
      val mapper = baseMapper().configure(DeserializationFeature.USE_LONG_FOR_INTS, true).build()
      readNode(mapper, json).isLong must beTrue
    }

    "read Int as BigInteger" >> {
      val json   = """{ "value" : 10 }"""
      val mapper = baseMapper().configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true).build()
      readNode(mapper, json).isBigInteger must beTrue
    }

    "read Long" >> {
      val json = s"""{ "value" : ${Long.MaxValue} }"""
      readNode(baseMapper().build(), json).isLong must beTrue
    }

    "read BigInteger" >> {
      val json = s"""{ "value" : ${Long.MaxValue}0000 }"""
      readNode(baseMapper().build(), json).isBigInteger must beTrue
    }

    "not advance the cursor excessively when re/entering tine Deserializer from a custom Deserializer on the Child of a Parent/Child class hierarchy" >> {
      // https://github.com/lagom/lagom/issues/3241
      val json = {
        """
          |{
          |  "createdAt": 1234,
          |  "child": {
          |    "updatedAt": 555,
          |    "updatedBy": "another-user"
          |  },
          |  "updatedBy": "some-user",
          |  "updatedAt": 5678
          |}
          |""".stripMargin
      }

      val mapper   = baseMapper().build()
      val jsonNode = mapper.readTree(json)
      jsonNode.get("createdAt").asLong() must equalTo(1234L)
      jsonNode.get("child").get("updatedAt").asLong() must equalTo(555)
      jsonNode.get("child").get("updatedBy").asText() must equalTo("another-user")
      jsonNode.get("updatedAt").asLong() must equalTo(5678L)
      jsonNode.get("updatedBy").asText() must equalTo("some-user")

      val actual   = mapper.readValue(json, classOf[Parent]);
      val expected = new Parent(1234, new Child(555, "another-user"), 5678, "some-user")
      actual.getCreatedAt must equalTo(expected.getCreatedAt)
      actual.getChild.getUpdatedAt must equalTo(expected.getChild.getUpdatedAt)
      actual.getChild.getUpdatedBy must equalTo(expected.getChild.getUpdatedBy)
      actual.getUpdatedAt must equalTo(expected.getUpdatedAt)
      actual.getUpdatedBy must equalTo(expected.getUpdatedBy)
    }

  }

}
