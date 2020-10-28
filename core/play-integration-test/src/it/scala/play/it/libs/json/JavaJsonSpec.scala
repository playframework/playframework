/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.libs.json

import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Optional
import java.util.OptionalInt

import com.fasterxml.jackson.annotation.JsonRawValue
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Request
import play.core.test.FakeRequest
import play.libs.Json
import play.mvc.Http
import play.mvc.Http.RequestBody

// Use an `ObjectMapper` which overrides some defaults
class PlayBindingNameJavaJsonSpec extends JavaJsonSpec {
  override val createObjectMapper: ObjectMapper = GuiceApplicationBuilder()
  // should be able to use `.play.` namespace to override configurations
  // for this `ObjectMapper`.
    .configure("akka.serialization.jackson.play.serialization-features.WRITE_DURATIONS_AS_TIMESTAMPS" -> true)
    .build()
    .injector
    .instanceOf[ObjectMapper]

  "ObjectMapper" should {
    "respect the custom configuration" in new JsonScope {
      Json.mapper().isEnabled(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS) must beTrue
    }
  }
}

// The dependency injected `ObjectMapper`
class ApplicationJavaJsonSpec extends JavaJsonSpec {
  override val createObjectMapper: ObjectMapper = GuiceApplicationBuilder().build().injector.instanceOf[ObjectMapper]

  "ObjectMapper" should {
    "not render date types as timestamps by default" in new JsonScope {
      Json.mapper().isEnabled(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS) must beFalse
      Json.mapper().isEnabled(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) must beFalse
    }
  }
}

// Classic static `ObjectMapper` from play.libs.Json
class StaticJavaJsonSpec extends JavaJsonSpec {
  override val createObjectMapper: ObjectMapper = Json.newDefaultMapper()
}

trait JavaJsonSpec extends Specification {
  sequential

  def createObjectMapper: ObjectMapper

  private[json] class JsonScope(val mapper: ObjectMapper = createObjectMapper) extends Scope {
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

      "serialize Java Optional fields" in new JsonScope {
        val optNumber = Optional.of[Integer](55555)
        val optInt    = OptionalInt.of(12345)

        // The configured mapper should be able to handle optional values
        Json.mapper().writeValueAsString(optNumber) must_== "55555"
        Json.mapper().writeValueAsString(optInt) must_== "12345"
      }

      "serialize Java Time field" in new JsonScope {
        val instant: Instant = Instant.ofEpochSecond(1425435861)

        // The configured mapper should be able to handle Java Time fields
        Json.mapper().writeValueAsString(instant) must_== """"2015-03-04T02:24:21Z""""
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

    "when deserializing to a POJO" should {
      "deserialize from request body" in new JsonScope(createObjectMapper) {
        val validRequest: Request[Http.RequestBody] =
          Request[Http.RequestBody](FakeRequest(), new RequestBody(testJson))
        val javaPOJO = validRequest.body.parseJson(classOf[JavaPOJO]).get()

        javaPOJO.getBar must_== "baz"
        javaPOJO.getFoo must_== "bar"
        javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861L)
        javaPOJO.getOptNumber must_== Optional.of(55555)
        javaPOJO.getOptionalInt must_== OptionalInt.of(12345)
      }

      "deserialize even if there are missing fields" in new JsonScope(createObjectMapper) {
        val testJsonMissingFields: Request[Http.RequestBody] =
          Request[Http.RequestBody](FakeRequest(), new RequestBody(mapper.createObjectNode()))
        testJsonMissingFields.body.parseJson(classOf[JavaPOJO]).get().getBar must_== null
      }

      "return empty when request body is not a JSON" in new JsonScope(createObjectMapper) {
        val testNotJsonBody: Request[Http.RequestBody] =
          Request[Http.RequestBody](FakeRequest(), new RequestBody("foo"))
        testNotJsonBody.body.parseJson(classOf[JavaPOJO]) must_== Optional.empty()
      }

      "ignore unknown fields" in new JsonScope(createObjectMapper) {
        val javaPOJO = Json.fromJson(testJson, classOf[JavaPOJO])
        javaPOJO.getBar must_== "baz"
        javaPOJO.getFoo must_== "bar"
        javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861L)
        javaPOJO.getOptNumber must_== Optional.of(55555)
      }
    }

    "when serializing from a POJO" should {
      "serialize to a response body" in new JsonScope(createObjectMapper) {
        val pojo = new JavaPOJO(
          "Foo String",
          "Bar String",
          Instant.ofEpochSecond(1425435861),
          Optional.of[Integer](55555),
          OptionalInt.of(12345)
        )
        val jsonNode: JsonNode = Json.toJson(pojo)

        // Regular fields
        jsonNode.get("foo").asText() must_== "Foo String"
        jsonNode.get("bar").asText() must_== "Bar String"

        // Optional fields
        jsonNode.get("optNumber").asText() must_== "55555"
        jsonNode.get("optionalInt").asText() must_== "12345"

        // Java Time fields
        jsonNode.get("instant").asText() must_== "2015-03-04T02:24:21Z"
      }

      "include null fields" in new JsonScope(createObjectMapper) {
        val pojo = new JavaPOJO(
          null,         // foo
          "Bar String", // bar
          Instant.ofEpochSecond(1425435861),
          Optional.of[Integer](55555),
          OptionalInt.of(12345)
        )
        val jsonNode: JsonNode = Json.toJson(pojo)

        jsonNode.has("foo") must beTrue
      }
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
