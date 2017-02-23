/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams.{ Subscriber, Subscription }

object SubscriberEvents {
  case object OnComplete
  case class OnError(t: Throwable)
  case class OnSubscribe(s: Subscription)
  case class OnNext(t: Int)
}

trait SubscriberEvents {
  self: EventRecorder =>

  import SubscriberEvents._

  object subscriber extends Subscriber[Int] {
    def onError(t: Throwable) = record(OnError(t))
    def onSubscribe(s: Subscription) = record(OnSubscribe(s))
    def onComplete() = record(OnComplete)
    def onNext(t: Int) = record(OnNext(t))
  }
}
