/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.http

import java.util.concurrent.{ Callable, CompletableFuture, CompletionStage, TimeUnit }
import javax.inject.{ Inject, Provider }

import akka.Done
import com.github.benmanes.caffeine.cache.{ Cache, Caffeine }
import com.google.common.primitives.Primitives
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.cache.caffeine.CaffeineCacheModule
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{ PlaySpecification, TestServer, WsTestClient }
import play.cache.{ Cached, DefaultAsyncCacheApi }
import play.inject.ApplicationLifecycle
import play.mvc.Result

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._
import scala.reflect.ClassTag

class JavaCachedActionSpec extends PlaySpecification with WsTestClient {

  def makeRequest[T](controller: MockController)(block: Port => T): T = {

    import play.api.inject.bind

    implicit val port = testServerPort
    lazy val app: Application = GuiceApplicationBuilder()
      .disable[CaffeineCacheModule]
      .bindings(
        bind[play.api.cache.AsyncCacheApi].toProvider[TestAsyncCacheApiProvider],
        bind[play.cache.AsyncCacheApi].to[DefaultAsyncCacheApi]
      )
      .routes {
        case _ => JAction(app, controller)
      }.build()

    running(TestServer(port, app)) {
      block(port)
    }
  }

  "Java CachedAction" should {

    "when controller is annotated" in {

      "cache result" in makeRequest(new CachedController()) { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )

        val first = responses.head
        val cached = responses.last

        first.status must beEqualTo(cached.status)
        first.body must beEqualTo(cached.body)
      }

      "expire result" in makeRequest(new CachedController()) { port =>

        val first = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        Thread.sleep(5.seconds.toMillis) // enough time to ensure the cache was expired

        val second = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        first.status must beEqualTo(second.status)
        first.body must not(beEqualTo(second.body))
      }

    }

    "when action is annotated" in {
      "cache result" in makeRequest(new MockController {
        @Cached(key = "play.it.http.MockController.MockController.cache", duration = 1 /* second */ )
        override def action: Result = play.mvc.Results.ok("Cached result: " + System.nanoTime())
      }) { port =>
        val responses = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), ""),
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        )

        val first = responses.head
        val cached = responses.last

        first.status must beEqualTo(cached.status)
        first.body must beEqualTo(cached.body)
      }

      "expire result" in makeRequest(new MockController {
        @Cached(key = "play.it.http.MockController.MockController.cache", duration = 1 /* second */ )
        override def action: Result = play.mvc.Results.ok("Cached result: " + System.nanoTime())
      }) { port =>

        val first = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        Thread.sleep(5.seconds.toMillis) // enough time to ensure the cache was expired

        val second = BasicHttpClient.makeRequests(port)(
          BasicRequest("GET", "/", "HTTP/1.1", Map(), "")
        ).head

        first.status must beEqualTo(second.status)
        first.body must not(beEqualTo(second.body))
      }
    }
  }
}

@Cached(key = "play.it.http.CachedController.cache", duration = 1 /* second */ )
class CachedController extends MockController {
  override def action: Result = {
    play.mvc.Results.ok("Cached result: " + System.currentTimeMillis())
  }
}

/**
 * This is necessary to avoid EhCache shutdown problems.
 *
 * Using Caffeine here since it is already a dependency and it handles expiration.
 */
class TestAsyncCacheApi(cache: Cache[String, Object])(implicit context: ExecutionContext) extends AsyncCacheApi {
  override def set(key: String, value: Any, expiration: Duration): Future[Done] = Future.successful {
    cache.put(key, value.asInstanceOf[Object])
    Done
  }

  override def remove(key: String): Future[Done] = Future {
    cache.invalidate(key)
    Done
  }

  override def getOrElseUpdate[A: ClassTag](key: String, expiration: Duration)(orElse: => Future[A]): Future[A] = {
    get[A](key).flatMap {
      case Some(value) => Future.successful(value)
      case None => orElse.flatMap(value => set(key, value, expiration).map(_ => value))
    }
  }

  override def get[T](key: String)(implicit ct: ClassTag[T]): Future[Option[T]] = {
    val result = Option(cache.getIfPresent(key)).filter { v =>
      Primitives.wrap(ct.runtimeClass).isInstance(v) ||
        ct == ClassTag.Nothing || (ct == ClassTag.Unit && v == ((): Unit))
    }.asInstanceOf[Option[T]]
    Future.successful(result)
  }

  override def removeAll(): Future[Done] = Future {
    cache.invalidateAll()
    Done
  }
}

class TestAsyncCacheApiProvider @Inject() (lifeCycle: ApplicationLifecycle)(implicit context: ExecutionContext) extends Provider[TestAsyncCacheApi] {
  override def get(): TestAsyncCacheApi = {
    val cache = Caffeine
      .newBuilder()
      .expireAfterWrite(1, TimeUnit.SECONDS) // consistent with the value used in @Cached annotations above
      .build[String, Object]()

    lifeCycle.addStopHook(new Callable[CompletionStage[_]] {
      override def call(): CompletionStage[_] = {
        cache.cleanUp()
        cache.invalidateAll()
        CompletableFuture.completedFuture(true)
      }
    })

    new TestAsyncCacheApi(cache)
  }
}