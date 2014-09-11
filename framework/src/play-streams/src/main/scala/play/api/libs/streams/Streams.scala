package play.api.libs.streams

import org.reactivestreams._
import play.api.libs.iteratee._
import scala.concurrent.{ ExecutionContext, Future, Promise }

/**
 * Methods to adapt Futures, Promises, Iteratees and Enumerators to
 * and from Reactive Streams' Publishers and Subscribers.
 */
object Streams {

  /**
   * Adapt a Future into a Publisher. For a successful Future the
   * Publisher will emit a single element with the value. For a failed
   * Future the Publisher will emit an onError event.
   */
  def futureToPublisher[T](fut: Future[T]): Publisher[T] = new impl.FuturePublisher(fut)

  /**
   * Adapt a Promise into a Subscriber. The Subscriber accepts a
   * single value which is used to complete the Promise. If the
   * Subscriber's onError method is called then the Promise is
   * completed with a failure.
   *
   * A call to onError after onNext is called will be ignored.
   */
  def promiseToSubscriber[T](prom: Promise[T]): Subscriber[T] = new impl.PromiseSubscriber(prom)

  /**
   * Adapt a Promise into a Processor, creating an Processor that
   * consumes a single element and publishes it. The Subscriber end
   * of the the Processor is created with `promiseToSubscriber`. The
   * Publisher end of the Processor is created with `futureToPublisher`.
   */
  def promiseToProcessor[T](prom: Promise[T]): Processor[T, T] = {
    val subr = promiseToSubscriber(prom)
    val pubr = futureToPublisher(prom.future)
    val proc = join(subr, pubr)
    proc
  }

  /**
   * Adapt an Iteratee to a Subscriber and a result Iteratee.
   *
   * The Subscriber requests
   * elements one-by-one and feeds them to each Iteratee Cont step. When the
   * Iteratee step is Done or Error, the Subscription is cancelled.
   *
   * The result iteratee will be fulfilled when either the Iteratee
   * or the Subscriber subscription completes. The result Iteratee
   * may be in a Cont, Done or Error state, or a fourth state that
   * will yield errors or failed Futures when its methods are called.
   *
   * Calls to onNext send an Input.El to the iteratee and calls to
   * onComplete send an Input.EOF. If onError is called then the
   * iteratee enters an invalid state.
   *
   * If the Iteratee is in a Done or Error state then it will cancel
   * the Subscription as soon as possible, but it may still receive
   * calls to onError or onComplete. These calls are ignored. Be
   * careful because this means that it is possible for the Subscriber
   * to "consume" events, even if the Iteratee doesn't.
   */
  def iterateeToSubscriber[T, U](iter: Iteratee[T, U]): (Subscriber[T], Iteratee[T, U]) = {
    val subr = new impl.IterateeSubscriber(iter)
    val resultIter = subr.result
    (subr, resultIter)
  }

  /**
   * Adapt an Iteratee to a Publisher, publishing its Done value. If
   * the iteratee is *not* Done then an exception is published.
   *
   * This method is similar in its effect to Iteratee.run, which
   * extracts the final value from an Iteratee. However, unlike
   * Iteratee.run, this method will not feed an EOF input to the
   * Iteratee.
   */
  def iterateeDoneToPublisher[T, U](iter: Iteratee[T, U]): Publisher[U] = {
    iterateeFoldToPublisher[T, U, U](iter, {
      case Step.Done(x, _) => Future.successful(x)
      case notDone: Step[T, U] => Future.failed(new Exception("Can only get value from Done iteratee: $notDone"))
    })(Execution.trampoline)
  }

  /**
   * Fold an Iteratee and publish its result. This method is used
   * by iterateeDoneToPublisher to extract the value of a Done iteratee.
   */
  private def iterateeFoldToPublisher[T, U, V](iter: Iteratee[T, U], f: Step[T, U] => Future[V])(implicit ec: ExecutionContext): Publisher[V] = {
    val fut: Future[V] = iter.fold(f)(ec.prepare)
    val pubr: Publisher[V] = futureToPublisher(fut)
    pubr
  }

  /**
   * Adapt an Iteratee to a Processor, which consumes input and then
   * yields the iteratee's Done value. It uses iterateeToSubscriber and
   * iterateeDoneToPublisher to create each end of the Processor.
   */
  def iterateeToProcessor[T, U](iter: Iteratee[T, U]): Processor[T, U] = {
    val (subr, resultIter) = iterateeToSubscriber(iter)
    val pubr = iterateeDoneToPublisher(resultIter)
    val proc = join(subr, pubr)
    proc
  }

  /**
   * Adapt an Enumerator to a Publisher. Each Subscriber will be
   * adapted to an Iteratee and applied to the Enumerator. Input of
   * type Input.El will result in calls to onNext. Input of type
   * Input.EOF will call onComplete and end the Subscription.
   *
   * If emptyElement is None then Input of type Input.Empty will
   * be ignored. If it is set to Some(x) then it will call onNext
   * with the value x.
   */
  def enumeratorToPublisher[T](enum: Enumerator[T], emptyElement: Option[T] = None): Publisher[T] =
    new impl.EnumeratorPublisher(enum, emptyElement)

  /**
   * Adapt a Publisher to an Enumerator. This is achieved by
   * adapting any Iteratees into Subscribers using the
   * iterateeToSubscriber method.
   */
  def publisherToEnumerator[T](pubr: Publisher[T]): Enumerator[T] =
    new impl.PublisherEnumerator(pubr)

  /**
   * Join a Subscriber and Publisher together to make a Processor.
   * The Processor delegates its Subscriber methods to the Subscriber
   * and its Publisher methods to the Publisher. The Processor
   * otherwise does nothing.
   */
  def join[T, U](subr: Subscriber[T], pubr: Publisher[U]): Processor[T, U] =
    new impl.SubscriberPublisherProcessor(subr, pubr)
}
