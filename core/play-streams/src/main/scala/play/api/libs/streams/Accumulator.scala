/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import akka.stream.Materializer
import akka.stream.scaladsl.BroadcastHub
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Keep
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source

import java.util.Optional
import scala.annotation.unchecked.{ uncheckedVariance => uV }
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.FutureConverters._
import scala.jdk.OptionConverters._
import scala.util.Failure
import scala.util.Success
import scala.util.Try

/**
 * An accumulator of elements into a future of a result.
 *
 * This is essentially a lightweight wrapper around a Sink that gets materialised to a Future, but provides convenient
 * methods for working directly with that future as well as transforming the input.
 */
sealed trait Accumulator[-E, +A] {

  /**
   * Map the result of this accumulator to something else.
   */
  def map[B](f: A => B)(implicit executor: ExecutionContext): Accumulator[E, B]

  /**
   * Map the result of this accumulator to a future of something else.
   */
  def mapFuture[B](f: A => Future[B])(implicit executor: ExecutionContext): Accumulator[E, B]

  /**
   * Recover from errors encountered by this accumulator.
   */
  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit executor: ExecutionContext): Accumulator[E, B]

  /**
   * Recover from errors encountered by this accumulator.
   */
  def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]])(
      implicit executor: ExecutionContext
  ): Accumulator[E, B]

  /**
   * Return a new accumulator that first feeds the input through the given flow before it goes through this accumulator.
   */
  def through[F](flow: Flow[F, E, _]): Accumulator[F, A]

  /**
   * Right associative operator alias for through.
   *
   * This can be used for a more fluent DSL that matches the flow of the data, for example:
   *
   * {{{
   *   val intAccumulator: Accumulator[Int, Unit] = ...
   *   val toInt = Flow[String].map(_.toInt)
   *   val stringAccumulator = toInt ~>: intAccumulator
   * }}}
   */
  def ~>:[F](flow: Flow[F, E, _]): Accumulator[F, A] = through(flow)

  /**
   * Run this accumulator by feeding in the given source.
   */
  def run(source: Source[E, _])(implicit materializer: Materializer): Future[A]

  /**
   * Run this accumulator by feeding nothing into it.
   */
  def run()(implicit materializer: Materializer): Future[A]

  /**
   * Run this accumulator by feeding a single element into it.
   */
  def run(elem: E)(implicit materializer: Materializer): Future[A]

  /**
   * Right associative operator alias for run.
   *
   * This can be used for a more fluent DSL that matches the flow of the data, for example:
   *
   * {{{
   *   val intAccumulator: Accumulator[Int, Int] = ...
   *   val source = Source(1 to 3)
   *   val intFuture = source ~>: intAccumulator
   * }}}
   */
  def ~>:(source: Source[E, _])(implicit materializer: Materializer): Future[A] = run(source)

  /**
   * Convert this accumulator to a Sink that gets materialised to a Future.
   */
  def toSink: Sink[E, Future[A]]

  /**
   * Convert this accumulator to a Java Accumulator.
   *
   * @return The Java accumulator.
   */
  def asJava: play.libs.streams.Accumulator[E @uV, A @uV]
}

/**
 * An accumulator backed by a sink.
 *
 * This is essentially a lightweight wrapper around a Sink that gets materialised to a Future, but provides convenient
 * methods for working directly with that future as well as transforming the input.
 */
private class SinkAccumulator[-E, +A](wrappedSink: => Sink[E, Future[A]]) extends Accumulator[E, A] {
  private lazy val sink: Sink[E, Future[A]] = wrappedSink

  def map[B](f: A => B)(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.map(f)))

  def mapFuture[B](f: A => Future[B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.flatMap(f)))

  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.recover(pf)))

  def recoverWith[B >: A](
      pf: PartialFunction[Throwable, Future[B]]
  )(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.recoverWith(pf)))

  def through[F](flow: Flow[F, E, _]): Accumulator[F, A] = new SinkAccumulator(flow.toMat(sink)(Keep.right))

  def run(source: Source[E, _])(implicit materializer: Materializer): Future[A] = source.toMat(sink)(Keep.right).run()
  def run()(implicit materializer: Materializer): Future[A]                     = run(Source.empty)
  def run(elem: E)(implicit materializer: Materializer): Future[A]              = run(Source.single(elem))

  def toSink: Sink[E, Future[A]] = sink

  def asJava: play.libs.streams.Accumulator[E @uV, A @uV] = {
    play.libs.streams.Accumulator.fromSink(sink.mapMaterializedValue(_.asJava).asJava)
  }
}

private class StrictAccumulator[-E, +A](handler: Option[E] => Future[A], val toSink: Sink[E, Future[A]])
    extends Accumulator[E, A] {
  private def mapMat[B](f: Future[A] => Future[B])(implicit executor: ExecutionContext): StrictAccumulator[E, B] = {
    new StrictAccumulator(handler.andThen(f), toSink.mapMaterializedValue(f))
  }

  override def map[B](f: A => B)(implicit executor: ExecutionContext): Accumulator[E, B] =
    mapMat { future =>
      future.value match {
        case Some(Success(a)) => Future.fromTry(Try(f(a))) // optimize already completed case
        case _                => future.map(f)
      }
    }

  def mapFuture[B](f: A => Future[B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    mapMat { future =>
      future.value match {
        case Some(Success(a)) => // optimize already completed case
          Try(f(a)) match {
            case Success(fut) => fut
            case Failure(ex)  => Future.failed(ex)
          }
        case _ => future.flatMap(f)
      }
    }

  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    mapMat { future =>
      future.value match {
        case Some(Success(_)) => future // optimize already completed case
        case _                => future.recover(pf)
      }
    }

  def recoverWith[B >: A](
      pf: PartialFunction[Throwable, Future[B]]
  )(implicit executor: ExecutionContext): Accumulator[E, B] =
    mapMat { future =>
      future.value match {
        case Some(Success(_)) => future // optimize already completed case
        case _                => future.recoverWith(pf)
      }
    }

  override def through[F](flow: Flow[F, E, _]): Accumulator[F, A] = {
    new SinkAccumulator(flow.toMat(toSink)(Keep.right))
  }

  override def run(source: Source[E, _])(implicit materializer: Materializer): Future[A] = source.runWith(toSink)
  override def run()(implicit materializer: Materializer): Future[A]                     = handler(None)
  override def run(elem: E)(implicit materializer: Materializer): Future[A]              = handler(Some(elem))

  override def asJava: play.libs.streams.Accumulator[E @uV, A @uV] =
    play.libs.streams.Accumulator.strict(
      (t: Optional[E]) => handler(t.toScala).asJava,
      toSink.mapMaterializedValue(_.asJava).asJava
    )
}

private class FlattenedAccumulator[-E, +A](future: Future[Accumulator[E, A]])(implicit materializer: Materializer)
    extends SinkAccumulator[E, A](Accumulator.futureToSink(future)) {
  override def run(source: Source[E, _])(implicit materializer: Materializer): Future[A] = {
    future.flatMap(_.run(source))(materializer.executionContext)
  }

  override def run()(implicit materializer: Materializer): Future[A] =
    future.flatMap(_.run())(materializer.executionContext)
}

object Accumulator {
  private[streams] def futureToSink[E, A](
      future: Future[Accumulator[E, A]]
  )(implicit materializer: Materializer): Sink[E, Future[A]] = {
    import Execution.Implicits.trampoline

    Sink
      .futureSink(
        future
          .recover {
            case error =>
              new SinkAccumulator(Sink.cancelled[E].mapMaterializedValue(_ => Future.failed(error)))
          }
          .map(_.toSink)
      )
      .mapMaterializedValue(_.flatten)
  }

  /**
   * Create a new accumulator from the given Sink.
   */
  def apply[E, A](sink: Sink[E, Future[A]]): Accumulator[E, A] = new SinkAccumulator(sink)

  /**
   * Create a done accumulator.
   *
   * The underlying sink will cancel as soon as its onSubscribe method is called, and the materialized value will be
   * an immediately available future of `a`.
   */
  def done[A](a: A): Accumulator[Any, A] = done(Future.successful(a))

  /**
   * Create a done accumulator.
   *
   * The underlying sink will cancel as soon as its onSubscribe method is called, and the materialized value will be
   * the passed in future.
   */
  def done[A](a: Future[A]): Accumulator[Any, A] =
    new StrictAccumulator[Any, A](_ => a, Sink.cancelled.mapMaterializedValue(_ => a))

  /**
   * Create an accumulator that is capable of handling the stream as a single, possibly empty, element, with a sink
   * provided as a fallback if the stream can't be expressed as a single element.
   *
   * This is intended to be able to be used to avoid a stream materialization for strict entities.
   */
  def strict[E, A](strictHandler: Option[E] => Future[A], toSink: Sink[E, Future[A]]): Accumulator[E, A] = {
    new StrictAccumulator(strictHandler, toSink)
  }

  /**
   * Create an accumulator that forwards the stream fed into it to the source it produces.
   *
   * This is useful for when you want to send the consumed stream to another API that takes a Source as input.
   *
   * Extreme care must be taken when using this accumulator - the source *must always* be materialized and consumed.
   * If it isn't, this could lead to resource leaks and deadlocks upstream.
   *
   * @return An accumulator that forwards the stream to the produced source.
   */
  def source[E]: Accumulator[E, Source[E, _]] = {
    source(2)
  }
  def source[E](bufferSize: Int): Accumulator[E, Source[E, _]] = {
    // If Akka streams ever provides Sink.source(), we should use that instead.
    // https://github.com/akka/akka/issues/18406
    // BroadcastHub comes cloes to what we need, however there is still overhead (but less than with publisher)
    new SinkAccumulator(BroadcastHub.sink[E](bufferSize).mapMaterializedValue(Future.successful))
  }

  /**
   * Flatten a future of an accumulator to an accumulator.
   */
  def flatten[E, A](future: Future[Accumulator[E, A]])(implicit materializer: Materializer): Accumulator[E, A] = {
    new FlattenedAccumulator[E, A](future)
  }
}
