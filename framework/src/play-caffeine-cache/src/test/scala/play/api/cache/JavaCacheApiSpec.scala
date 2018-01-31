/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.util.concurrent.{ Callable, CompletableFuture, CompletionStage }

import akka.util.Timeout

import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.AsResult

import play.api.test.{ PlaySpecification, WithApplication }
import play.cache.{ AsyncCacheApi => JavaAsyncCacheApi, SyncCacheApi => JavaSyncCacheApi }

import scala.compat.java8.FutureConverters._
import scala.concurrent.duration._

class JavaCacheApiSpec(implicit ee: ExecutionEnv) extends PlaySpecification {
  private def after2sec[T: AsResult](result: => T): T = eventually(2, 2.seconds)(result)
  implicit val timeout: Timeout = 1.second

  sequential

  "Java AsyncCacheApi" should {
    "set cache values" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      await(cacheApi.set("foo", "bar").toScala)
      cacheApi.get[String]("foo").toScala must beEqualTo("bar").await
    }
    "set cache values with an expiration time" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      await(cacheApi.set("foo", "bar", 1 /* second */ ).toScala)

      after2sec { cacheApi.get[String]("foo").toScala must beNull.await }
    }
    "set cache values with an expiration time" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      await(cacheApi.set("foo", "bar", 10 /* seconds */ ).toScala)

      after2sec { cacheApi.get[String]("foo").toScala must beEqualTo("bar").await }
    }
    "get or update" should {
      "get value when it exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar").toScala)
        cacheApi.get[String]("foo").toScala must beEqualTo("bar").await
      }
      "update cache when value does not exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        val future = cacheApi.getOrElseUpdate[String]("foo", new Callable[CompletionStage[String]] {
          override def call() = CompletableFuture.completedFuture[String]("bar")
        }).toScala

        future must beEqualTo("bar").await
        cacheApi.get[String]("foo").toScala must beEqualTo("bar").await
      }
      "update cache with an expiration time when value does not exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        val future = cacheApi.getOrElseUpdate[String]("foo", new Callable[CompletionStage[String]] {
          override def call() = CompletableFuture.completedFuture[String]("bar")
        }, 1 /* second */ ).toScala

        future must beEqualTo("bar").await

        after2sec { cacheApi.get[String]("foo").toScala must beNull.await }
      }
    }
    "remove values from cache" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      await(cacheApi.set("foo", "bar").toScala)
      cacheApi.get[String]("foo").toScala must beEqualTo("bar").await

      await(cacheApi.remove("foo").toScala)
      cacheApi.get[String]("foo").toScala must beNull.await
    }

    "remove all values from cache" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      await(cacheApi.set("foo", "bar").toScala)
      cacheApi.get[String]("foo").toScala must beEqualTo("bar").await

      await(cacheApi.removeAll().toScala)
      cacheApi.get[String]("foo").toScala must beNull.await
    }
  }

  "Java SyncCacheApi" should {
    "set cache values" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
      cacheApi.set("foo", "bar")
      cacheApi.get[String]("foo") must beEqualTo("bar")
    }
    "set cache values with an expiration time" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
      cacheApi.set("foo", "bar", 1 /* second */ )

      cacheApi.get[String]("foo") must beNull.eventually(3, 2.seconds)
    }
    "set cache values with an expiration time" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
      cacheApi.set("foo", "bar", 10 /* seconds */ )

      after2sec { cacheApi.get[String]("foo") must beEqualTo("bar") }
    }
    "get or update" should {
      "get value when it exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        cacheApi.set("foo", "bar")
        cacheApi.get[String]("foo") must beEqualTo("bar")
      }
      "update cache when value does not exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        val value = cacheApi.getOrElseUpdate[String]("foo", new Callable[String] {
          override def call() = "bar"
        })

        value must beEqualTo("bar")
        cacheApi.get[String]("foo") must beEqualTo("bar")
      }
      "update cache with an expiration time when value does not exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        val future = cacheApi.getOrElseUpdate[String]("foo", new Callable[String] {
          override def call() = "bar"
        }, 1 /* second */ )

        future must beEqualTo("bar")

        after2sec { cacheApi.get[String]("foo") must beNull }
      }
    }
    "remove values from cache" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
      cacheApi.set("foo", "bar")
      cacheApi.get[String]("foo") must beEqualTo("bar")

      cacheApi.remove("foo")
      cacheApi.get[String]("foo") must beNull
    }
  }
}
