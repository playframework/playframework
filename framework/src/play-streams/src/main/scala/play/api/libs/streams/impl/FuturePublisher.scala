/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.concurrent.StateMachine
import play.api.libs.iteratee.Execution
import scala.concurrent.Future
import scala.util.{ Failure, Success, Try }

/*
 * Creates Subscriptions that link Subscribers to a Future.
 */
private[streams] trait FutureSubscriptionFactory[T] extends SubscriptionFactory[T] {

  def fut: Future[T]

  override def createSubscription[U >: T](
    subr: Subscriber[U],
    onSubscriptionEnded: SubscriptionHandle[U] => Unit) = {
    new FutureSubscription[T, U](fut, subr, onSubscriptionEnded)
  }

}

/**
 * Adapts an Future to a Publisher.
 */
private[streams] final class FuturePublisher[T](
  val fut: Future[T]) extends RelaxedPublisher[T] with FutureSubscriptionFactory[T]

private[streams] object FutureSubscription {
  /**
   * Possible states of the Subscription.
   */
  sealed trait State
  /**
   * A Subscription which hasn't had any elements requested.
   */
  final case object AwaitingRequest extends State
  /**
   * A Subscription that has had at least one element requested. We only care
   * that the value requested is >1 because a Future will only emit a single
   * value.
   */
  final case object Requested extends State
  /**
   * A Subscription completed by the Publisher.
   */
  final case object Completed extends State
  /**
   * A Subscription cancelled by the Subscriber.
   */
  final case object Cancelled extends State
}

import FutureSubscription._

private[streams] class FutureSubscription[T, U >: T](
  fut: Future[T],
  subr: Subscriber[U],
  onSubscriptionEnded: SubscriptionHandle[U] => Unit)
    extends StateMachine[State](initialState = AwaitingRequest)
    with Subscription with SubscriptionHandle[U] {

  // SubscriptionHandle methods

  override def start(): Unit = {
    fut.value match {
      case Some(Failure(t)) =>
        subscriber.onError(t)
        onSubscriptionEnded(this)
      case _ =>
        subscriber.onSubscribe(this)
    }
  }

  override def isActive: Boolean = state match {
    case AwaitingRequest | Requested => true
    case Cancelled | Completed => false
  }

  override def subscriber: Subscriber[U] = subr

  // Streams methods

  override def request(elements: Long): Unit = {
    if (elements <= 0) throw new IllegalArgumentException(s"The number of requested elements must be > 0: requested $elements elements")
    exclusive {
      case AwaitingRequest =>
        state = Requested
        // When we receive a request for an element, we trigger a call to
        // onFutureCompleted. We call it immediately if we can, otherwise we
        // schedule the call for when the Future is completed.
        fut.value match {
          case Some(result) =>
            onFutureCompleted(result)
          case None =>
            // Safe to use trampoline because onFutureCompleted only schedules async operations
            fut.onComplete(onFutureCompleted)(Execution.trampoline)
        }
      case _ =>
        ()
    }
  }

  override def cancel(): Unit = exclusive {
    case AwaitingRequest =>
      state = Cancelled
    case Requested =>
      state = Cancelled
    case _ =>
      ()
  }

  /**
   * Called when both an element has been requested and the Future is
   * completed. Calls onNext/onComplete or onError on the Subscriber.
   */
  private def onFutureCompleted(result: Try[T]): Unit = exclusive {
    case AwaitingRequest =>
      throw new IllegalStateException("onFutureCompleted shouldn't be called when in state AwaitingRequest")
    case Requested =>
      state = Completed
      result match {
        case Success(null) =>
          subr.onError(new NullPointerException("Future completed with a null value that cannot be sent by a Publisher"))
        case Success(value) =>
          subr.onNext(value)
          subr.onComplete()
        case Failure(t) =>
          subr.onError(t)
      }
      onSubscriptionEnded(this)
    case Cancelled =>
      ()
    case Completed =>
      throw new IllegalStateException("onFutureCompleted shouldn't be called when already in state Completed")
  }
}
