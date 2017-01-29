/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.cache

import java.util.concurrent.{ Callable, CompletableFuture, CompletionStage }

import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration._
import scala.compat.java8.FutureConverters._
import play.cache.{ AsyncCacheApi => JavaAsyncCacheApi, SyncCacheApi => JavaSyncCacheApi }
import play.api.test.{ PlaySpecification, WithApplication }

import scala.concurrent.Await

class JavaCacheApiSpec(implicit ee: ExecutionEnv) extends PlaySpecification {

  sequential

  "Java AsyncCacheApi" should {
    "set cache values" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      Await.result(cacheApi.set("foo", "bar").toScala, 1.second)
      cacheApi.get[String]("foo").toScala must beEqualTo("bar").await
    }
    "set cache values with an expiration time" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      Await.result(cacheApi.set("foo", "bar", 1 /* second */ ).toScala, 1.second)

      Thread.sleep(2.seconds.toMillis)
      cacheApi.get[String]("foo").toScala must beNull.await
    }
    "get or update" should {
      "get value when it exists" in new WithApplication {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        Await.result(cacheApi.set("foo", "bar").toScala, 1.second)
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

        Thread.sleep(2.seconds.toMillis)
        cacheApi.get[String]("foo").toScala must beNull.await
      }
    }
    "remove values from cache" in new WithApplication {
      val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
      Await.result(cacheApi.set("foo", "bar").toScala, 1.second)
      cacheApi.get[String]("foo").toScala must beEqualTo("bar").await

      Await.result(cacheApi.remove("foo").toScala, 1.second)
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

      Thread.sleep(2.seconds.toMillis)
      cacheApi.get[String]("foo") must beNull
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

        Thread.sleep(2.seconds.toMillis)
        cacheApi.get[String]("foo") must beNull
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
