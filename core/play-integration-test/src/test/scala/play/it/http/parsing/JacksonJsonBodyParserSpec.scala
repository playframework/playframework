/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http.parsing

import java.util.concurrent.TimeUnit

import com.fasterxml.jackson.databind.JsonNode
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.ByteString
import org.specs2.execute.Failure
import org.specs2.matcher.Matchers
import play.api.test._
import play.api.Application
import play.api.Configuration
import play.libs.F
import play.mvc.BodyParser
import play.mvc.Http
import play.mvc.Result
import play.test.Helpers

class JacksonJsonBodyParserSpec extends PlaySpecification with Matchers {

  // Jackson Json support in Play relies on static
  // global variables so these tests must run sequentially
  sequential

  private def tolerantJsonBodyParser(implicit app: Application): BodyParser[JsonNode] =
    app.injector.instanceOf(classOf[BodyParser.TolerantJson])

  "The JSON body parser" should {
    def parse(json: String)(
        implicit mat: Materializer,
        app: Application
    ): F.Either[Result, JsonNode] = {
      val encoding: String                = "utf-8"
      val bodyParser                      = tolerantJsonBodyParser
      val fakeRequest: Http.RequestHeader = Helpers.fakeRequest().header(CONTENT_TYPE, "application/json").build()
      await(
        bodyParser(fakeRequest).asScala().run(Source.single(ByteString(json.getBytes(encoding))))
      )
    }

    "parse a simple JSON body with custom Jackson json-read-features" in new WithApplication(guiceBuilder =>
      guiceBuilder.configure(
        "pekko.serialization.jackson.play.json-read-features.ALLOW_SINGLE_QUOTES" -> "true"
      )
    ) {
      override def running() = {

        val configuration: Configuration = implicitly[Application].configuration
        configuration.get[Boolean](
          "pekko.serialization.jackson.play.json-read-features.ALLOW_SINGLE_QUOTES"
        ) must beTrue

        val either: F.Either[Result, JsonNode] = parse("""{ 'field1':'value1' }""")
        either.left.ifPresent(verboseFailure)
        either.right.get().get("field1").asText() must_=== "value1"
      }
    }

    "parse very deep JSON bodies" in new WithApplication() {
      override def running() = {
        // other tests may have changed the default mapper, so we reset it
        play.libs.Json.setObjectMapper(play.libs.Json.newDefaultMapper())
        val depth                              = 50000
        val either: F.Either[Result, JsonNode] = parse(s"""{"foo": ${"[" * depth} "asdf" ${"]" * depth}  }""")
        either.left.ifPresent(verboseFailure)
        var node: JsonNode = either.right.get().at("/foo")
        while (node.isArray) {
          node = node.get(0)
        }

        node.asText() must_== "asdf"
      }
    }

  }

  def verboseFailure(result: Result)(implicit mat: Materializer): Failure = {
    val errorMessage = s"""Parse failure. Play-produced error HTML page: 
                          | ${resultToString(result)}
                          |""".stripMargin
    failure(errorMessage)
  }

  def resultToString(r: Result)(implicit mat: Materializer): String = {
    r.body()
      .consumeData(mat)
      .toCompletableFuture
      .get(6, TimeUnit.SECONDS)
      .utf8String
  }

}
