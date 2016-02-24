/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._

/**
 * Very simple Processor that delegates to its Subscriber and Publisher arguments.
 */
final class SubscriberPublisherProcessor[T, U](subr: Subscriber[T], pubr: Publisher[U]) extends Processor[T, U] {
  override def subscribe(subscriber: Subscriber[_ >: U]): Unit = pubr.subscribe(subscriber)
  override def onSubscribe(subscription: Subscription): Unit = subr.onSubscribe(subscription)
  override def onError(cause: Throwable): Unit = subr.onError(cause)
  override def onComplete(): Unit = subr.onComplete()
  override def onNext(element: T): Unit = subr.onNext(element)
}
