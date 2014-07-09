package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.concurrent.StateMachine
import play.api.libs.iteratee.Execution
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Adapts a Future to a Publisher.
 */
private[streams] final class FuturePublisher[T](fut: Future[T]) extends AbstractPublisher[T, FuturePublisherSubscription[T]] {

  override protected def createSubscription(subr: Subscriber[T]) = new FuturePublisherSubscription(this, subr, fut)
  override protected def onSubscriptionAdded(subscription: FuturePublisherSubscription[T]): Unit = {
    fut.value match {
      case Some(Failure(t)) =>
        subscription.subscriber.onError(t)
        removeSubscription(subscription)
      case _ =>
        subscription.subscriber.onSubscribe(subscription)
    }
  }

}

private[streams] object FuturePublisherSubscription {
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

import FuturePublisherSubscription._

private[streams] class FuturePublisherSubscription[T](pubr: FuturePublisher[T], subr: Subscriber[T], fut: Future[T])
    extends StateMachine[State](initialState = AwaitingRequest) with CheckableSubscription[T] {

  // CheckableSubscription methods

  override def isActive: Boolean = state match {
    case AwaitingRequest | Requested => true
    case Cancelled | Completed => false
  }

  override def subscriber: Subscriber[T] = subr

  // Streams methods

  override def request(elements: Int): Unit = {
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
      pubr.removeSubscription(this)
    case Cancelled =>
      ()
    case Completed =>
      throw new IllegalStateException("onFutureCompleted shouldn't be called when already in state Completed")
  }
}