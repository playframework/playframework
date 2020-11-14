/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.mvc

import java.util.Optional

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import akka.testkit.TestKit
import akka.util.ByteString
import com.fasterxml.jackson.core.io.CharacterEscapes
import com.fasterxml.jackson.core.io.SerializedString
import com.fasterxml.jackson.core.JsonEncoding
import org.specs2.mutable.SpecificationLike
import org.specs2.specification.BeforeAfterAll
import play.libs.Json
import play.mvc.Http.HeaderNames

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

class StatusHeaderSpec extends TestKit(ActorSystem("StatusHeaderSpec")) with SpecificationLike with BeforeAfterAll {
  override def beforeAll(): Unit = {}

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
    Json.mapper.getFactory.setCharacterEscapes(null)
  }

  "StatusHeader" should {
    "use factory attached to Json.mapper() when serializing Json" in {
      val materializer = Materializer.matFromSystem

      Json.mapper.getFactory.setCharacterEscapes(new CharacterEscapes {
        override def getEscapeSequence(ch: Int) = new SerializedString(f"\\u$ch%04x")

        override def getEscapeCodesForAscii: Array[Int] =
          CharacterEscapes.standardAsciiEscapesForJSON.zipWithIndex.map {
            case (_, code) if !(Character.isAlphabetic(code) || Character.isDigit(code)) =>
              CharacterEscapes.ESCAPE_CUSTOM
            case (escape, _) => escape
          }
      })

      val jsonNode = Json.mapper.createObjectNode
      jsonNode.put("field", "value&")

      val statusHeader = new StatusHeader(Http.Status.OK)
      val result       = statusHeader.sendJson(jsonNode, JsonEncoding.UTF8)

      val content = Await.result(for {
        byteString <- result.body.dataStream.runWith(Sink.head[ByteString], materializer)
      } yield byteString.decodeString("UTF-8"), Duration.Inf)

      content must_== "{\"field\":\"value\\u0026\"}"
      result.contentType() must_== Optional.of("application/json")
      result.header(HeaderNames.CONTENT_DISPOSITION) must_== Optional.empty()
    }
  }
}
