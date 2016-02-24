/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._

/**
 * A Publisher which subscribes Subscribers without performing any
 * checking about whether he Suscriber is already subscribed. This
 * makes RelaxedPublisher a bit faster, but possibly a bit less safe,
 * than a CheckingPublisher.
 */
private[streams] abstract class RelaxedPublisher[T] extends Publisher[T] {
  self: SubscriptionFactory[T] =>

  // Streams method
  final override def subscribe(subr: Subscriber[_ >: T]): Unit = {
    val handle: SubscriptionHandle[_] = createSubscription(subr, RelaxedPublisher.onSubscriptionEndedNop)
    handle.start()
  }

}

private[streams] object RelaxedPublisher {
  val onSubscriptionEndedNop: Any => Unit = _ => ()
}
