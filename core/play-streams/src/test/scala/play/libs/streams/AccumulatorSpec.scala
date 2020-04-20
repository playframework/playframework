/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.libs.streams

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

import akka.NotUsed

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.compat.java8.FutureConverters
import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.stream.javadsl.Sink
import akka.stream.Materializer
import akka.japi.function.{ Function => JFn }
import org.reactivestreams.Subscription

class AccumulatorSpec extends org.specs2.mutable.Specification {
  import scala.collection.JavaConverters._

  def withMaterializer[T](block: Materializer => T): T = {
    val system = ActorSystem("test")
    try {
      block(Materializer.matFromSystem(system))
    } finally {
      system.terminate()
      Await.result(system.whenTerminated, Duration.Inf)
    }
  }

  def sum: Accumulator[Int, Int] = Accumulator.fromSink(Sink.fold[Int, Int](0, (a, b) => a + b))

  def source: Source[Int, NotUsed] = Source.from((1 to 3).asJava)
  def await[T](f: Future[T]): T    = Await.result(f, 10.seconds)
  def await[T](f: CompletionStage[T]): T =
    f.toCompletableFuture.get(10, TimeUnit.SECONDS)

  def errorSource[T]: Source[T, NotUsed] =
    Source.fromPublisher(s => {
      s.onSubscribe(new Subscription {
        def cancel(): Unit         = s.onComplete()
        def request(n: Long): Unit = s.onError(new RuntimeException("error"))
      })
    })

  "an accumulator" should {
    "be flattenable from a future of itself" in {
      "for a successful future" in withMaterializer { m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable.complete(sum)

        await(fAcc.run(source, m)) must_== 6
      }

      "for a failed future" in withMaterializer { implicit m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable.completeExceptionally(new RuntimeException("failed"))

        await(fAcc.run(source, m)) must throwA[ExecutionException].like {
          case ex =>
            val cause = ex.getCause
            (cause.isInstanceOf[RuntimeException] must beTrue).and(cause.getMessage must_== "failed")
        }
      }

      "for a failed stream" in withMaterializer { implicit m =>
        val completable = new CompletableFuture[Accumulator[Int, Int]]()

        val fAcc = Accumulator.flatten[Int, Int](completable, m)
        completable.complete(sum)

        await(fAcc.run(errorSource[Int], m)) must throwA[ExecutionException].like {
          case ex =>
            val cause = ex.getCause
            (cause.isInstanceOf[RuntimeException] must beTrue).and(cause.getMessage must_== "error")
        }
      }
    }

    "be compatible with Java accumulator" in {
      "Java asScala" in withMaterializer { implicit m =>
        val sink = sum.toSink.mapMaterializedValue(new JFn[CompletionStage[Int], Future[Int]] {
          def apply(f: CompletionStage[Int]): Future[Int] =
            FutureConverters.toScala(f)
        })

        await(play.api.libs.streams.Accumulator(sink.asScala).run(source.asScala)) must_== 6
      }
    }
  }
}
