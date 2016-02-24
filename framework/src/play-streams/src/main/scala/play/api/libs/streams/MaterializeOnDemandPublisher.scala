/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams

import akka.stream.Materializer
import akka.stream.scaladsl.{ Sink, Source }
import org.reactivestreams.{ Publisher, Subscriber, Subscription }
import play.api.libs.concurrent.StateMachine

private[play] object MaterializeOnDemandPublisher {
  sealed trait State

  /**
   * The publisher is waiting for demand from the subscriber.
   */
  case object AwaitingDemand extends State

  /**
   * The publisher is caching demand from the subscriber.
   *
   * At this point, the source has been materialized with a forwarding subscriber, but it has not
   * yet invoked the onSubscribe method on that subscriber.
   */
  case class CachingDemand(demand: Long) extends State

  /**
   * The subscriber has cancelled.
   */
  case object Cancelled extends State

  /**
   * Demand is being forwarded through the subscription obtained by the forwarding subscriber.
   */
  case class ForwardingDemand(subscription: Subscription) extends State
}

import MaterializeOnDemandPublisher._

/**
 * A publisher that only materializes a flow to the given source when its subscriber signals demand.
 *
 * If the subscriber never signals demand (ie, it just cancels), the source will never be materialized.
 *
 * This is used to work around https://github.com/akka/akka/issues/18013.
 */
private[play] class MaterializeOnDemandPublisher[T](source: Source[T, _])(implicit mat: Materializer) extends StateMachine[State](AwaitingDemand) with Publisher[T] {

  def subscribe(subscriber: Subscriber[_ >: T]) = {
    subscriber.onSubscribe(new ForwardingSubscription(subscriber))
  }

  class ForwardingSubscription(subscriber: Subscriber[_ >: T]) extends Subscription {
    def cancel() = exclusive {
      case ForwardingDemand(subscription) =>
        state = Cancelled
        subscription.cancel()
      case _ =>
        // If we're still awaiting demand, we go cancelled, and the source is never materialized.
        // If we're already cancelled, we stay cancelled.
        // If we're caching demand, we short circuit that, and just cancel.
        state = Cancelled
    }

    def request(n: Long) = exclusive {
      case AwaitingDemand =>
        state = CachingDemand(n)
        source.runWith(Sink.fromSubscriber(new ForwardingSubscriber(subscriber)))
      case CachingDemand(demand) =>
        state = CachingDemand(n + demand)
      case Cancelled =>
      // nop, as required by the spec
      case ForwardingDemand(subscription) =>
        subscription.request(n)
    }
  }

  class ForwardingSubscriber[S >: T](subscriber: Subscriber[S]) extends Subscriber[S] {

    def onSubscribe(subscription: Subscription) = exclusive {
      case CachingDemand(demand) =>
        state = ForwardingDemand(subscription)
        subscription.request(demand)
      case Cancelled =>
        state = ForwardingDemand(subscription)
        subscription.cancel()
      case ForwardingDemand(_) =>
        throw new IllegalStateException("Subscribe invoked twice")
      case AwaitingDemand =>
        throw new IllegalStateException("Impossible state: awaiting demand when a forwarding subscriber has already been created")
    }

    def onError(t: Throwable) = subscriber.onError(t)

    def onComplete() = subscriber.onComplete()

    def onNext(s: S) = subscriber.onNext(s)
  }
}
