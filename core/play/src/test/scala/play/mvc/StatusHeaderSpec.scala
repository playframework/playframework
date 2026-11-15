/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.util.Optional

import scala.concurrent.duration.Duration
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.stream.Materializer
import org.apache.pekko.testkit.TestKit
import org.apache.pekko.util.ByteString
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import play.libs.Json
import play.mvc.Http.HeaderNames
import tools.jackson.core.io.CharacterEscapes
import tools.jackson.core.io.SerializedString
import tools.jackson.core.json.JsonFactory
import tools.jackson.core.JsonEncoding
import tools.jackson.databind.json.JsonMapper

class StatusHeaderSpec extends TestKit(ActorSystem("StatusHeaderSpec")) with SpecificationLike with BeforeAfterAll {
  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    Json.setObjectMapper(null)
  }

  "StatusHeader" should {
    "use factory attached to Json.mapper() when serializing Json" in {
      val materializer = Materializer.matFromSystem

      val characterEscapes = new CharacterEscapes {
        override def getEscapeSequence(ch: Int): SerializedString = new SerializedString(f"\\u$ch%04x")

        override def getEscapeCodesForAscii: Array[Int] =
          CharacterEscapes.standardAsciiEscapesForJSON.zipWithIndex.map {
            case (_, code) if !(Character.isAlphabetic(code) || Character.isDigit(code)) =>
              CharacterEscapes.ESCAPE_CUSTOM
            case (escape, _) => escape
          }
      }
      Json.setObjectMapper(JsonMapper.builder(JsonFactory.builder().characterEscapes(characterEscapes).build()).build())

      val jsonNode = Json.mapper.createObjectNode
      jsonNode.put("field", "value&")

      val statusHeader = new StatusHeader(Http.Status.OK)
      val result       = statusHeader.sendJson(jsonNode, JsonEncoding.UTF8)

      val content = Await.result(
        for {
          byteString <- result.body.dataStream.runWith(Sink.head[ByteString], materializer)
        } yield byteString.decodeString("UTF-8"),
        Duration.Inf
      )

      content must_== "{\"field\":\"value\\u0026\"}"
      result.contentType() must_== Optional.of("application/json")
      result.header(HeaderNames.CONTENT_DISPOSITION) must_== Optional.empty()
    }
  }
}
