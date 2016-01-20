package play.api.libs.streams

import akka.stream.Materializer
import akka.stream.scaladsl.{ Source, Keep, Flow, Sink }
import org.reactivestreams.{ Publisher, Subscription, Subscriber }

import scala.concurrent.{ Promise, ExecutionContext, Future }
import scala.util.{ Failure, Success }
import scala.compat.java8.FutureConverters._

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
  def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]])(implicit executor: ExecutionContext): Accumulator[E, B]

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

  import scala.annotation.unchecked.{ uncheckedVariance => uV }

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
private class SinkAccumulator[-E, +A](sink: Sink[E, Future[A]]) extends Accumulator[E, A] {

  def map[B](f: A => B)(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.map(f)))

  def mapFuture[B](f: A => Future[B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.flatMap(f)))

  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.recover(pf)))

  def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]])(implicit executor: ExecutionContext): Accumulator[E, B] =
    new SinkAccumulator(sink.mapMaterializedValue(_.recoverWith(pf)))

  def through[F](flow: Flow[F, E, _]): Accumulator[F, A] =
    new SinkAccumulator(flow.toMat(sink)(Keep.right))

  def run(source: Source[E, _])(implicit materializer: Materializer): Future[A] = {
    source.toMat(sink)(Keep.right).run()
  }

  def run()(implicit materializer: Materializer): Future[A] = {
    run(Source.empty)
  }

  def toSink: Sink[E, Future[A]] = sink

  import scala.annotation.unchecked.{ uncheckedVariance => uV }

  def asJava: play.libs.streams.Accumulator[E @uV, A @uV] = {
    play.libs.streams.Accumulator.fromSink(sink.asJava)
  }
}

/**
 * An accumulator that ignores the body passed in, and is immediately available.
 */
private class DoneAccumulator[+A](future: Future[A]) extends Accumulator[Any, A] {

  def map[B](f: A => B)(implicit executor: ExecutionContext): Accumulator[Any, B] =
    new DoneAccumulator(future.map(f))

  def mapFuture[B](f: A => Future[B])(implicit executor: ExecutionContext): Accumulator[Any, B] =
    new DoneAccumulator(future.flatMap(f))

  def recover[B >: A](pf: PartialFunction[Throwable, B])(implicit executor: ExecutionContext): Accumulator[Any, B] =
    new DoneAccumulator(future.recover(pf))

  def recoverWith[B >: A](pf: PartialFunction[Throwable, Future[B]])(implicit executor: ExecutionContext): Accumulator[Any, B] =
    new DoneAccumulator(future.recoverWith(pf))

  def through[F](flow: Flow[F, Any, _]): Accumulator[F, A] = this

  def run(source: Source[Any, _])(implicit materializer: Materializer): Future[A] = {
    source.toMat(Sink.cancelled)((_, _) => future).run()
  }

  def run()(implicit materializer: Materializer): Future[A] = future

  def toSink: Sink[Any, Future[A]] = Sink.cancelled.mapMaterializedValue(_ => future)

  import scala.annotation.unchecked.{ uncheckedVariance => uV }

  def asJava: play.libs.streams.Accumulator[Any @uV, A @uV] = {
    play.libs.streams.Accumulator.done(future.toJava)
  }
}

object Accumulator {

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
  def done[A](a: A): Accumulator[Any, A] = new DoneAccumulator[A](Future.successful(a))

  /**
   * Create a done accumulator.
   *
   * The underlying sink will cancel as soon as its onSubscribe method is called, and the materialized value will be
   * the passed in future.
   */
  def done[A](a: Future[A]): Accumulator[Any, A] = new DoneAccumulator(a)

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
    // If Akka streams ever provides Sink.source(), we should use that instead.
    // https://github.com/akka/akka/issues/18406
    new SinkAccumulator(Sink.asPublisher[E](fanout = false).mapMaterializedValue(publisher => Future.successful(Source.fromPublisher(publisher))))
  }

  /**
   * Flatten a future of an accumulator to an accumulator.
   */
  def flatten[E, A](future: Future[Accumulator[E, A]])(implicit materializer: Materializer): Accumulator[E, A] = {
    import play.api.libs.iteratee.Execution.Implicits.trampoline

    // Ideally, we'd use the following code, except due to akka streams bugs...
    // new Accumulator(Sink.publisher[E].mapMaterializedValue { publisher =>
    //  future.recover {
    //    case error => new Accumulator(Sink.cancelled[E].mapMaterializedValue(_ => Future.failed(error)))
    //  }.flatMap { accumulator =>
    //    Source(publisher).toMat(accumulator.toSink)(Keep.right).run()
    //  }
    //})

    val result = Promise[A]()
    val sink = Sink.fromSubscriber(new Subscriber[E] {
      @volatile var subscriber: Subscriber[_ >: E] = _

      def onSubscribe(sub: Subscription) = future.onComplete {
        case Success(accumulator) =>
          Source.fromPublisher(new Publisher[E]() {
            def subscribe(s: Subscriber[_ >: E]) = {
              subscriber = s
              s.onSubscribe(sub)
            }
          }).runWith(accumulator.toSink.mapMaterializedValue { fA =>
            result.completeWith(fA)
          })
        case Failure(error) =>
          sub.cancel()
          result.failure(error)
      }

      def onError(t: Throwable) = subscriber.onError(t)
      def onComplete() = subscriber.onComplete()
      def onNext(t: E) = subscriber.onNext(t)
    })

    new SinkAccumulator(sink.mapMaterializedValue(_ => result.future))
  }

}
