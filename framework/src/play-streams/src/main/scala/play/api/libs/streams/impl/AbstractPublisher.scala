package play.api.libs.streams.impl

import java.util.concurrent.atomic.AtomicReference
import org.reactivestreams._
import play.api.libs.iteratee.Execution
import scala.annotation.tailrec
import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success, Try }

/**
 * Partial Publisher implementation that does the work of managing a list of
 * current subscriptions. Publishers that extend this class must provide
 * a Subscription implementation that extends CheckableSubscription.
 *
 * When `subscribe` is called, a Subscription will be created. If the
 * Subscriber doesn't have any existing Subscriptions then the Subscription
 * will be registered internally and the method `onSubscriptionAdded` will
 * be called for subclasses to act upon.
 *
 * Subclasses should call `removeSubscription` when a Subscription ends.
 */
private[streams] abstract class AbstractPublisher[T, S <: CheckableSubscription[T, _]] extends Publisher[T] {

  /**
   * Called by `subscribe` to create a CheckableSubscription. The subscription
   * may fail, so subclasses should wait until `onSubscriptionAdded` is called
   * before they do anything with it.
   */
  protected def createSubscription[U >: T](subr: Subscriber[U]): S

  /**
   * Called when a new Subscription is added.
   */
  protected def onSubscriptionAdded(subscription: S): Unit

  /**
   * The list of currently registered Subscriptions.
   */
  private val subscriptions = new AtomicReference[List[S]](Nil)

  // Streams method
  final override def subscribe(subr: Subscriber[_ >: T]): Unit = {
    val subscription = createSubscription(subr)

    @tailrec
    def addSubscription(): Unit = {
      val oldSubscriptions = subscriptions.get
      if (oldSubscriptions.exists(s => (s.subscriber eq subr) && s.isActive)) {
        subr.onError(new IllegalStateException("Subscriber is already subscribed to this Publisher"))
      } else {
        val newSubscriptions: List[S] = subscription :: oldSubscriptions
        if (subscriptions.compareAndSet(oldSubscriptions, newSubscriptions)) {
          onSubscriptionAdded(subscription)
        } else addSubscription()
      }
    }
    addSubscription()
  }

  /**
   * Remove a currently registered Subscription. Must be called by a
   * subclass when a Subscription ends. No other classes should call
   * this method.
   */
  @tailrec
  final private[streams] def removeSubscription(subscription: S): Unit = {
    val oldSubscriptions = subscriptions.get
    val newSubscriptions = oldSubscriptions.filterNot(_.subscriber eq subscription.subscriber)
    if (subscriptions.compareAndSet(oldSubscriptions, newSubscriptions)) () else removeSubscription(subscription)
  }

}

/**
 * A Subscription that knows its Subscriber and whether or not it is currently
 * active. Used by AbstractPublisher.
 */
private[streams] trait CheckableSubscription[T, U >: T] extends Subscription {

  /**
   * The Subscriber for this Subscription.
   */
  def subscriber: Subscriber[U]

  /**
   * Whether or not this Subscription is active. It won't be active if it has
   * been cancelled, completed or had an error.
   */
  def isActive: Boolean
}