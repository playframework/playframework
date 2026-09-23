/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise

import org.apache.pekko.stream.scaladsl.Source
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.test._

class CaffeineCachedSpec extends PlaySpecification {
  sequential

  "the cached action" should {
    "populate a missing result only once for concurrent requests" in new WithApplication() {
      override def running() = {
        val cached     = new Cached(app.injector.instanceOf[AsyncCacheApi])(using app.materializer)
        val invoked    = new AtomicInteger()
        val result     = Promise[Result]()
        val underlying = EssentialAction { _ =>
          invoked.incrementAndGet()
          Accumulator.done(result.future)
        }
        val action    = cached.everything(_ => "single-flight").build(underlying)
        val responses = Vector.fill(20) {
          action(FakeRequest()).run()(app.materializer)
        }

        eventually(20, 50.millis)(invoked.get() must_== 1)
        result.success(Results.Ok("shared"))
        responses.foreach(response => contentAsString(response) must_== "shared")
        invoked.get() must_== 1
      }
    }

    "not share a non-cacheable streamed response between concurrent requests" in new WithApplication() {
      override def running() = {
        implicit val executionContext: ExecutionContext = app.materializer.executionContext
        val cached                                      = new Cached(app.injector.instanceOf[AsyncCacheApi])(using app.materializer)
        val invoked                                     = new AtomicInteger()
        val release                                     = Promise[Unit]()
        val underlying                                  = EssentialAction { _ =>
          val invocation = invoked.incrementAndGet()
          Accumulator.done(
            release.future.map(_ => Results.NotFound.chunked(Source.single(s"missing-$invocation")))
          )
        }
        val action    = cached.status(_ => "not-retained", OK, 1.minute).build(underlying)
        val responses = Vector.fill(10) {
          action(FakeRequest()).run()(app.materializer)
        }

        eventually(20, 50.millis)(invoked.get() must_== 1)
        release.success(())
        responses
          .map(response => contentAsString(response)(using defaultAwaitTimeout, app.materializer))
          .toSet
          .size must_==
          responses.size
        invoked.get() must_== responses.size
      }
    }
  }
}
