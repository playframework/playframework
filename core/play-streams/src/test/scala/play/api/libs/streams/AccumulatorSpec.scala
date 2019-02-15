/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import java.util.concurrent.CompletionStage

import akka.actor.ActorSystem
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.{ ActorMaterializer, Materializer }
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import org.specs2.mutable.Specification

import scala.compat.java8.FutureConverters
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

class AccumulatorSpec extends Specification {

  def withMaterializer[T](block: Materializer => T) = {
    val system = ActorSystem("test")
    try {
      block(ActorMaterializer()(system))
    } finally {
      system.terminate()
      Await.result(system.whenTerminated, Duration.Inf)
    }
  }

  def source = Source(1 to 3)
  def await[T](f: Future[T]) = Await.result(f, 10.seconds)
  def error[T](any: Any): T = throw sys.error("error")
  def errorSource[T] = Source.fromPublisher(new Publisher[T] {
    def subscribe(s: Subscriber[_ >: T]) = {
      s.onSubscribe(new Subscription {
        def cancel() = s.onComplete()
        def request(n: Long) = s.onError(new RuntimeException("error"))
      })
    }
  })

  "a sink accumulator" should {
    def sum: Accumulator[Int, Int] = Accumulator(Sink.fold[Int, Int](0)(_ + _))

    "provide map" in withMaterializer { implicit m =>
      await(sum.map(_ + 10).run(source)) must_== 16
    }

    "provide mapFuture" in withMaterializer { implicit m =>
      await(sum.mapFuture(r => Future(r + 10)).run(source)) must_== 16
    }

    "be recoverable" in {

      "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
        await(sum.map(error[Int]).recover {
          case e => 20
        }.run(source)) must_== 20
      }

      "when the exception comes fom the stream" in withMaterializer { implicit m =>
        await(sum.recover {
          case e => 20
        }.run(errorSource)) must_== 20
      }
    }

    "be recoverable with a future" in {

      "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
        await(sum.map(error[Int]).recoverWith {
          case e => Future(20)
        }.run(source)) must_== 20
      }

      "when the exception comes from the stream" in withMaterializer { implicit m =>
        await(sum.recoverWith {
          case e => Future(20)
        }.run(errorSource)) must_== 20
      }
    }

    "be able to be composed with a flow" in withMaterializer { implicit m =>
      await(sum.through(Flow[Int].map(_ * 2)).run(source)) must_== 12
    }

    "be able to be composed in a left to right associate way" in withMaterializer { implicit m =>
      await(source ~>: Flow[Int].map(_ * 2) ~>: sum) must_== 12
    }

    "be flattenable from a future of itself" in {

      "for a successful future" in withMaterializer { implicit m =>
        await(Accumulator.flatten(Future(sum)).run(source)) must_== 6
      }

      "for a failed future" in withMaterializer { implicit m =>
        val result = Accumulator.flatten[Int, Int](Future.failed(new RuntimeException("failed"))).run(source)
        await(result) must throwA[RuntimeException]("failed")
      }

      "for a failed stream" in withMaterializer { implicit m =>
        await(Accumulator.flatten(Future(sum)).run(errorSource)) must throwA[RuntimeException]("error")
      }
    }

    "be compatible with Java accumulator" in {
      "Java asScala" in withMaterializer { implicit m =>
        await(play.libs.streams.Accumulator.fromSink(sum.toSink.mapMaterializedValue(FutureConverters.toJava).asJava[Int, CompletionStage[Int]]).asScala().run(source)) must_== 6
      }

      "Scala asJava" in withMaterializer { implicit m =>
        await(FutureConverters.toScala(sum.asJava.run(source.asJava, m))) must_== 6
      }
    }
  }

  "a strict accumulator" should {
    def sum: Accumulator[Int, Int] = Accumulator.strict[Int, Int](e => Future.successful(e.getOrElse(0)), Sink.fold[Int, Int](0)(_ + _))

    "run with a stream" in {

      "provide map" in withMaterializer { implicit m =>
        await(sum.map(_ + 10).run(source)) must_== 16
      }

      "provide mapFuture" in withMaterializer { implicit m =>
        await(sum.mapFuture(r => Future(r + 10)).run(source)) must_== 16
      }

      "be recoverable" in {

        "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
          await(sum.map(error[Int]).recover {
            case e => 20
          }.run(source)) must_== 20
        }

        "when the exception comes fom the stream" in withMaterializer { implicit m =>
          await(sum.recover {
            case e => 20
          }.run(errorSource)) must_== 20
        }
      }

      "be recoverable with a future" in {

        "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
          await(sum.map(error[Int]).recoverWith {
            case e => Future(20)
          }.run(source)) must_== 20
        }

        "when the exception comes from the stream" in withMaterializer { implicit m =>
          await(sum.recoverWith {
            case e => Future(20)
          }.run(errorSource)) must_== 20
        }
      }

      "be able to be composed with a flow" in withMaterializer { implicit m =>
        await(sum.through(Flow[Int].map(_ * 2)).run(source)) must_== 12
      }

      "be able to be composed in a left to right associate way" in withMaterializer { implicit m =>
        await(source ~>: Flow[Int].map(_ * 2) ~>: sum) must_== 12
      }

      "be flattenable from a future of itself" in {

        "for a successful future" in withMaterializer { implicit m =>
          await(Accumulator.flatten(Future(sum)).run(source)) must_== 6
        }

        "for a failed future" in withMaterializer { implicit m =>
          val result = Accumulator.flatten[Int, Int](Future.failed(new RuntimeException("failed"))).run(source)
          await(result) must throwA[RuntimeException]("failed")
        }

        "for a failed stream" in withMaterializer { implicit m =>
          await(Accumulator.flatten(Future(sum)).run(errorSource)) must throwA[RuntimeException]("error")
        }
      }

      "be compatible with Java accumulator" in {
        "Java asScala" in withMaterializer { implicit m =>
          await(play.libs.streams.Accumulator.fromSink(sum.toSink.mapMaterializedValue(FutureConverters.toJava).asJava[Int, CompletionStage[Int]]).asScala().run(source)) must_== 6
        }

        "Scala asJava" in withMaterializer { implicit m =>
          await(FutureConverters.toScala(sum.asJava.run(source.asJava, m))) must_== 6
        }
      }
    }

    "run with a single element" in {

      "provide map" in withMaterializer { implicit m =>
        await(sum.map(_ + 10).run(6)) must_== 16
      }

      "provide mapFuture" in withMaterializer { implicit m =>
        await(sum.mapFuture(r => Future(r + 10)).run(6)) must_== 16
      }

      "be recoverable" in {
        "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
          await(sum.map(error[Int]).recover {
            case e => 20
          }.run(6)) must_== 20
        }
      }

      "be recoverable with a future" in {
        "when the exception is introduced in the materialized value" in withMaterializer { implicit m =>
          await(sum.map(error[Int]).recoverWith {
            case e => Future(20)
          }.run(6)) must_== 20
        }
      }

      "be able to be composed with a flow" in withMaterializer { implicit m =>
        await(sum.through(Flow[Int].map(_ * 2)).run(6)) must_== 12
      }

      "be flattenable from a future of itself" in {

        "for a successful future" in withMaterializer { implicit m =>
          await(Accumulator.flatten(Future(sum)).run(6)) must_== 6
        }
      }

      "be compatible with Java accumulator" in {
        "Java asScala" in withMaterializer { implicit m =>
          await(play.libs.streams.Accumulator.fromSink(sum.toSink.mapMaterializedValue(FutureConverters.toJava).asJava[Int, CompletionStage[Int]]).asScala().run(6)) must_== 6
        }

        "Scala asJava" in withMaterializer { implicit m =>
          await(FutureConverters.toScala(sum.asJava.run(6, m))) must_== 6
        }
      }
    }

  }

}
