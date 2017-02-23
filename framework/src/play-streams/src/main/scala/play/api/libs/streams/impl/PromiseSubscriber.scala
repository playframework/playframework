/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.concurrent.StateMachine
import scala.concurrent.Promise

private[streams] object PromiseSubscriber {
  /**
   * Internal state of the Subscriber.
   */
  sealed trait State
  /**
   * A Subscriber that hasn't had onSubscribe called on it yet, and whose
   * Promise is not complete.
   */
  final case object AwaitingSubscription extends State
  /**
   * A Subscriber that has had onSubscribe called on and whose
   * Promise is not complete.
   */
  final case object Subscribed extends State
  /**
   * A Subscriber that is complete, either because onComplete or onError was
   * called or because its Promise is complete.
   */
  final case object Completed extends State
}

import PromiseSubscriber._

// Assume that promise's onComplete handler runs asynchronously
private[streams] class PromiseSubscriber[T](prom: Promise[T])
    extends StateMachine[State](initialState = AwaitingSubscription) with Subscriber[T] {

  // Streams methods

  override def onSubscribe(subscription: Subscription): Unit = exclusive {
    case Subscribed =>
      throw new IllegalStateException("Can't call onSubscribe twice")
    case AwaitingSubscription =>
      // Check if promise is completed. Even if we request elements, we
      // still need to handle the Promise completing in some other way.
      if (prom.isCompleted) {
        state = Completed
        subscription.cancel()
      } else {
        state = Subscribed
        subscription.request(1)
      }
    case Completed =>
      subscription.cancel()
  }

  override def onError(cause: Throwable): Unit = exclusive {
    case AwaitingSubscription | Subscribed =>
      state = Completed
      prom.failure(cause) // we assume any Future.onComplete handlers run asynchronously
    case Completed =>
      ()
  }

  override def onComplete(): Unit = exclusive {
    case AwaitingSubscription | Subscribed =>
      prom.failure(new IllegalStateException("Can't handle onComplete until an element has been received"))
      state = Completed
    case Completed =>
      ()
  }

  override def onNext(element: T): Unit = exclusive {
    case AwaitingSubscription =>
      state = Completed
      throw new IllegalStateException("Can't handle onNext until at least one subscription has occurred")
    case Subscribed =>
      state = Completed
      prom.success(element) // we assume any Future.onComplete handlers run asynchronously
    case Completed =>
      ()
  }

}
