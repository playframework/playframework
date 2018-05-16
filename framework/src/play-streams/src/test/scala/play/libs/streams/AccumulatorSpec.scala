/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.streams

import java.util.concurrent.{
  CompletableFuture,
  CompletionStage,
  ExecutionException,
  TimeUnit
}

import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.compat.java8.FutureConverters

import akka.actor.ActorSystem
import akka.stream.javadsl.{ Source, Sink }
import akka.stream.{ ActorMaterializer, Materializer }
import akka.japi.function.{ Function => JFn, Function2 => JFn2 }

import org.reactivestreams.{ Subscription, Subscriber, Publisher }

class AccumulatorSpec extends org.specs2.mutable.Specification {
  // JavaConversions is required because JavaConverters.asJavaIterable only exists in 2.12
  // and we cross compile for 2.11
  import scala.collection.JavaConverters._

  def withMaterializer[T](block: Materializer => T) = {
    val system = ActorSystem("test")
    try {
      block(ActorMaterializer()(system))
    } finally {
      system.terminate()
      Await.result(system.whenTerminated, Duration.Inf)
    }
  }

  def sum: Accumulator[Int, Int] =
    Accumulator.fromSink(Sink.fold[Int, Int](
      0,
      new JFn2[Int, Int, Int] { def apply(a: Int, b: Int) = a + b }))

  def source = Source from (1 to 3).asJava
  def sawait[T](f: Future[T]) = Await.result(f, 10.seconds)
  def await[T](f: CompletionStage[T]) =
    f.toCompletableFuture.get(10, TimeUnit.SECONDS)

  def errorSource[T] = Source.fromPublisher(new Publisher[T] {
    def subscribe(s: Subscriber[_ >: T]) = {
      s.onSubscribe(new Subscription {
        def cancel() = s.onComplete()
        def request(n: Long) = s.onError(new RuntimeException("error"))
      })
    }
  })

  "an accumulator" should {
    "be flattenable from a future of itself" in {
      "for a successful future" in withMaterializer { m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable complete sum

        await(fAcc.run(source, m)) must_== 6
      }

      "for a failed future" in withMaterializer { implicit m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable.completeExceptionally(new RuntimeException("failed"))

        await(fAcc.run(source, m)) must throwA[ExecutionException].like {
          case ex =>
            val cause = ex.getCause
            cause.isInstanceOf[RuntimeException] must beTrue and (
              cause.getMessage must_== "failed")
        }
      }

      "for a failed stream" in withMaterializer { implicit m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable complete sum

        await(fAcc.run(errorSource[Int], m)) must throwA[ExecutionException].like {
          case ex =>
            val cause = ex.getCause
            cause.isInstanceOf[RuntimeException] must beTrue and (
              cause.getMessage must_== "error")
        }
      }
    }

    "be compatible with Java accumulator" in {
      "Java asScala" in withMaterializer { implicit m =>
        val sink = sum.toSink.mapMaterializedValue(
          new JFn[CompletionStage[Int], Future[Int]] {
            def apply(f: CompletionStage[Int]): Future[Int] =
              FutureConverters.toScala(f)
          })

        sawait(play.api.libs.streams.Accumulator(sink.asScala).
          run(source.asScala)) must_== 6
      }
    }
  }
}
