/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import java.util.concurrent.atomic.AtomicReference
import org.reactivestreams._
import scala.annotation.tailrec

/**
 * A Publisher which checks that it never starts more than one active
 * Subscription for the same Subscriber. This extra safety comes at a
 * little bit of extra cost. If you want a cheaper Publisher, use
 * RelaxedPublisher.
 */
private[streams] abstract class CheckingPublisher[T] extends Publisher[T] {
  self: SubscriptionFactory[T] =>

  /**
   * The list of handles to currently active Subscriptions.
   */
  private val subscriptions = new AtomicReference[List[SubscriptionHandle[_]]](Nil)

  // Streams method
  final override def subscribe(subr: Subscriber[_ >: T]): Unit = {
    val handle: SubscriptionHandle[_] = createSubscription(subr, removeSubscription)

    @tailrec
    def addSubscription(): Unit = {
      val oldSubscriptions = subscriptions.get
      if (oldSubscriptions.exists(s => (s.subscriber eq subr) && s.isActive)) {
        subr.onError(new IllegalStateException("Subscriber is already subscribed to this Publisher"))
      } else {
        val newSubscriptions: List[SubscriptionHandle[_]] = handle :: oldSubscriptions
        if (subscriptions.compareAndSet(oldSubscriptions, newSubscriptions)) {
          handle.start()
        } else addSubscription()
      }
    }
    addSubscription()
  }

  @tailrec
  private def removeSubscription(subscription: SubscriptionHandle[_]): Unit = {
    val oldSubscriptions = subscriptions.get
    val newSubscriptions = oldSubscriptions.filterNot(_.subscriber eq subscription.subscriber)
    if (subscriptions.compareAndSet(oldSubscriptions, newSubscriptions)) () else removeSubscription(subscription)
  }

}
