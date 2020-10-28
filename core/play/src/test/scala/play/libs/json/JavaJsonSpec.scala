/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Optional

import com.fasterxml.jackson.databind.ObjectMapper
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.Request
import play.core.test.FakeRequest
import play.mvc.Http
import play.mvc.Http.RequestBody

class JavaJsonSpec extends Specification {
  sequential

  private class JsonScope(val mapper: ObjectMapper = new ObjectMapper()) extends Scope {
    val testJsonString =
      """{
        |  "foo" : "bar",
        |  "bar" : "baz",
        |  "instant" : 1425435861,
        |  "optNumber" : 55555,
        |  "optionalInt" : 12345,
        |  "float" : 2.5,
        |  "double" : 1.7976931348623157E308,
        |  "copyright" : "\u00a9",
        |  "baz" : [ 1, 2, 3 ]
        |}""".stripMargin.replaceAll("\r?\n", System.lineSeparator)

    val testJsonInputStream = new ByteArrayInputStream(testJsonString.getBytes("UTF-8"))

    val testJson = mapper.createObjectNode()
    testJson
      .put("foo", "bar")
      .put("bar", "baz")
      .put("instant", 1425435861)
      .put("optNumber", 55555)
      .put("optionalInt", 12345)
      .put("float", 2.5)
      .put("double", 1.7976931348623157e308) // Double.MaxValue
      .put("copyright", "\u00a9")            // copyright symbol
      .set("baz", mapper.createArrayNode().add(1).add(2).add(3))

    Json.setObjectMapper(mapper)
  }

  "Json" should {
    "use the correct object mapper" in new JsonScope {
      Json.mapper() must_== mapper
    }
    "parse" in {
      "from string" in new JsonScope {
        Json.parse(testJsonString) must_== testJson
      }
      "from UTF-8 byte array" in new JsonScope {
        Json.parse(testJsonString.getBytes("UTF-8")) must_== testJson
      }
      "from InputStream" in new JsonScope {
        Json.parse(testJsonInputStream) must_== testJson
      }

      "string into wrapped objects" in new JsonScope {
        val json =
          """
            |{
            | "code": "1234",
            | "city": "NYC",
            | "street": "Manhattan Av."
            |}
            |""".stripMargin

        private val jsonNode: JsonNode = Json.parse(json)
        private val customer: Customer = Json.fromJson(jsonNode, classOf[Customer])
        customer.code must_== "1234"
        customer.address must_== new Address("NYC", "Manhattan Av.")
      }
    }
    "stringify" in {
      "stringify" in new JsonScope {
        Json.stringify(testJson) must_== Json.stringify(Json.parse(testJsonString))
      }
      "asciiStringify" in new JsonScope {
        val resultString = Json.stringify(Json.parse(testJsonString)).replace("\u00a9", "\\u00A9")
        Json.asciiStringify(testJson) must_== resultString
      }
      "prettyPrint" in new JsonScope {
        Json.prettyPrint(testJson) must_== testJsonString
      }
      "unwrapped objects" in new JsonScope {
        val c = new Customer("1234")
        c.address = new Address("NYC", "Manhattan Av.")
        val json = Json.stringify(Json.toJson(c))
        json must_== """{"code":"1234","city":"NYC","street":"Manhattan Av."}"""

      }

      "embedded/raw object" in new JsonScope {

        val expected =
          """{"id":"abcd","contents":
            | "contents": { 
            |   "items": [
            |     {"id":"t-shirt", "count": 3},
            |     {"id":"mug",     "count": 5}
            |   ]
            |  }
            |}""".stripMargin

        val raw =
          """
            | "contents": { 
            |   "items": [
            |     {"id":"t-shirt", "count": 3},
            |     {"id":"mug",     "count": 5}
            |   ]
            |  }
            |""".stripMargin
        val cart = new ShoppingCart("abcd", raw)
        val json = Json.stringify(Json.toJson(cart))

        json must_== expected

      }

    }
    "deserialize to a POJO from request body" in new JsonScope(Json.newDefaultMapper()) {
      val validRequest: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(testJson))
      val javaPOJO                                = validRequest.body.parseJson(classOf[JavaPOJO]).get()

      javaPOJO.getBar must_== "baz"
      javaPOJO.getFoo must_== "bar"
      javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861L)
      javaPOJO.getOptNumber must_== Optional.of(55555)

      val testNotJsonBody: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody("foo"))
      testNotJsonBody.body.parseJson(classOf[JavaPOJO]) must_== Optional.empty()

      val testJsonMissingFields: Request[Http.RequestBody] =
        Request[Http.RequestBody](FakeRequest(), new RequestBody(mapper.createObjectNode()))
      testJsonMissingFields.body.parseJson(classOf[JavaPOJO]).get().getBar must_== null
    }
    "ignore unknown fields when deserializing to a POJO" in new JsonScope(Json.newDefaultMapper()) {
      val javaPOJO = Json.fromJson(testJson, classOf[JavaPOJO])
      javaPOJO.getBar must_== "baz"
      javaPOJO.getFoo must_== "bar"
      javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861L)
      javaPOJO.getOptNumber must_== Optional.of(55555)
    }
  }
}

/// Below this point there's a handful of classes useful to test certain Jackson edge cases. These classes
/// are not wrapped on an object or use any scala-idiomatic feature on purpose. The reasons are (1) to ensure
/// no Jackson modules for scala-specific code are interfering, and (2) to avoid hitting cases Jackson
/// doesn't support such as inner classes being non-static or similar.
class Address(var city: String, var street: String) {
  override def toString             = s"Address(city=$city, street=$street)"
  def canEqual(other: Any): Boolean = other.isInstanceOf[Address]
  override def equals(other: Any): Boolean = other match {
    case that: Address =>
      (that.canEqual(this)) &&
        city == that.city &&
        street == that.street
    case _ => false
  }
  override def hashCode(): Int = {
    val state = Seq(city, street)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
class Customer(var code: String) {
  @JsonUnwrapped var address: Address = null

  override def toString             = s"Customer(code=$code, address=$address)"
  def canEqual(other: Any): Boolean = other.isInstanceOf[Customer]
  override def equals(other: Any): Boolean = other match {
    case that: Customer =>
      (that.canEqual(this)) &&
        code == that.code &&
        address == that.address
    case _ => false
  }
  override def hashCode(): Int = {
    val state = Seq(code, address)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

// @JsonRawValue is a serialization-only annotation so this model can only be used in serialization tests.
// see https://github.com/FasterXML/jackson-annotations/wiki/Jackson-Annotations#serialization-details
class ShoppingCart(var id: String, @JsonRawValue var contents: String) {
  override def toString             = s"ShoppingCart(id=$id, contents=$contents)"
  def canEqual(other: Any): Boolean = other.isInstanceOf[ShoppingCart]
  override def equals(other: Any): Boolean = other match {
    case that: ShoppingCart =>
      (that.canEqual(this)) &&
        id == that.id &&
        contents == that.contents
    case _ => false
  }
  override def hashCode(): Int = {
    val state = Seq(id, contents)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
