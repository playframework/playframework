/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
        |  "a" : 2.5,
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
      .put("a", 2.5)
      .put("copyright", "\u00a9") // copyright symbol
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
    }
    "deserialize to a POJO from request body" in new JsonScope(Json.newDefaultMapper()) {

      val validRequest: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(testJson))
      val javaPOJO = validRequest.body.parseJson(classOf[JavaPOJO]).get()

      javaPOJO.getBar must_== "baz"
      javaPOJO.getFoo must_== "bar"
      javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861l)
      javaPOJO.getOptNumber must_== Optional.of(55555)

      val testNotJsonBody: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody("foo"))
      testNotJsonBody.body.parseJson(classOf[JavaPOJO]) must_== Optional.empty()

      val testJsonMissingFields: Request[Http.RequestBody] = Request[Http.RequestBody](FakeRequest(), new RequestBody(mapper.createObjectNode()))
      testJsonMissingFields.body.parseJson(classOf[JavaPOJO]).get().getBar must_== null
    }
    "ignore unknown fields when deserializing to a POJO" in new JsonScope(Json.newDefaultMapper()) {
      val javaPOJO = Json.fromJson(testJson, classOf[JavaPOJO])
      javaPOJO.getBar must_== "baz"
      javaPOJO.getFoo must_== "bar"
      javaPOJO.getInstant must_== Instant.ofEpochSecond(1425435861l)
      javaPOJO.getOptNumber must_== Optional.of(55555)
    }
  }
}
