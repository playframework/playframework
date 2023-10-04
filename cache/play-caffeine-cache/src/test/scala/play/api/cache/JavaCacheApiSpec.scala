/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cache

import java.util.concurrent.CompletableFuture
import java.util.Optional

import scala.concurrent.duration._
import scala.jdk.FutureConverters._

import org.apache.pekko.util.Timeout
import org.specs2.concurrent.ExecutionEnv
import org.specs2.execute.AsResult
import play.api.test.PlaySpecification
import play.api.test.WithApplication
import play.cache.{ AsyncCacheApi => JavaAsyncCacheApi }
import play.cache.{ SyncCacheApi => JavaSyncCacheApi }

class JavaCacheApiSpec(implicit ee: ExecutionEnv) extends PlaySpecification {
  private def after2sec[T: AsResult](result: => T): T = eventually(2, 2.seconds)(result)
  implicit val timeout: Timeout                       = 1.second
  private val oneSecondExpiration                     = 1
  private val tenSecondsExpiration                    = 10

  sequential

  "Java AsyncCacheApi" should {
    "set cache values" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar").asScala)
        cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await
      }
    }
    "set cache values with an expiration time" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar", oneSecondExpiration).asScala)

        after2sec { cacheApi.get[String]("foo").asScala must beEqualTo(Optional.empty()).await }
      }
    }
    "set cache values with an expiration time" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar", tenSecondsExpiration).asScala)

        after2sec {
          cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await
        }
      }
    }
    "get or update" should {
      "get value when it exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
          await(cacheApi.set("foo", "bar").asScala)
          cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await
        }
      }
      "update cache when value does not exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
          val future = cacheApi
            .getOrElseUpdate[String]("foo", () => CompletableFuture.completedFuture[String]("bar"))
            .asScala

          future must beEqualTo("bar").await
          cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await
        }
      }
      "update cache with an expiration time when value does not exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
          val future = cacheApi
            .getOrElseUpdate[String]("foo", () => CompletableFuture.completedFuture[String]("bar"), oneSecondExpiration)
            .asScala

          future must beEqualTo("bar").await

          after2sec {
            cacheApi.get[String]("foo").asScala must beEqualTo(Optional.empty()).await
          }
        }
      }
    }
    "remove values from cache" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar").asScala)
        cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await

        await(cacheApi.remove("foo").asScala)
        cacheApi.get[String]("foo").asScala must beEqualTo(Optional.empty()).await
      }
    }

    "remove all values from cache" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaAsyncCacheApi]
        await(cacheApi.set("foo", "bar").asScala)
        cacheApi.get[String]("foo").asScala must beEqualTo(Optional.of("bar")).await

        await(cacheApi.removeAll().asScala)
        cacheApi.get[String]("foo").asScala must beEqualTo(Optional.empty()).await
      }
    }
  }

  "Java SyncCacheApi" should {
    "set cache values" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        cacheApi.set("foo", "bar")
        cacheApi.get[String]("foo") must beEqualTo(Optional.of("bar"))
      }
    }
    "set cache values with an expiration time" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        cacheApi.set("foo", "bar", oneSecondExpiration)

        cacheApi.get[String]("foo") must beEqualTo(Optional.empty()).eventually(3, 2.seconds)
      }
    }
    "set cache values with an expiration time" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        cacheApi.set("foo", "bar", tenSecondsExpiration)

        after2sec {
          cacheApi.get[String]("foo") must beEqualTo(Optional.of("bar"))
        }
      }
    }
    "get or update" should {
      "get value when it exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
          cacheApi.set("foo", "bar")
          cacheApi.get[String]("foo") must beEqualTo(Optional.of("bar"))
        }
      }
      "update cache when value does not exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
          val value    = cacheApi.getOrElseUpdate[String]("foo", () => "bar")

          value must beEqualTo("bar")
          cacheApi.get[String]("foo") must beEqualTo(Optional.of("bar"))
        }
      }
      "update cache with an expiration time when value does not exists" in new WithApplication {
        override def running() = {
          val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
          val future   = cacheApi.getOrElseUpdate[String]("foo", () => "bar", oneSecondExpiration)

          future must beEqualTo("bar")

          after2sec {
            cacheApi.get[String]("foo") must beEqualTo(Optional.empty())
          }
        }
      }
    }
    "remove values from cache" in new WithApplication {
      override def running() = {
        val cacheApi = app.injector.instanceOf[JavaSyncCacheApi]
        cacheApi.set("foo", "bar")
        cacheApi.get[String]("foo") must beEqualTo(Optional.of("bar"))

        cacheApi.remove("foo")
        cacheApi.get[String]("foo") must beEqualTo(Optional.empty())
      }
    }
  }
}
