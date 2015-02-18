/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import scala.concurrent._

object PublisherEvents {
  case class RequestMore(elementCount: Long)
  case object Cancel
}

trait PublisherEvents[T] {
  self: EventRecorder =>

  import PublisherEvents._

  case class BoundedSubscription[T, U >: T](pr: Publisher[T], sr: Subscriber[U]) extends Subscription {
    def cancel(): Unit = {
      record(Cancel)
    }
    def request(elements: Long): Unit = {
      record(RequestMore(elements))
    }
    def onNext(element: T): Unit = sr.onNext(element)
  }

  object publisher extends Publisher[T] {
    val subscription = Promise[BoundedSubscription[T, _]]()
    override def subscribe(sr: Subscriber[_ >: T]) = {
      subscription.success(BoundedSubscription(this, sr))
    }
  }

  private def forSubscription(f: BoundedSubscription[T, _] => Any)(implicit ec: ExecutionContext): Future[Unit] = {
    publisher.subscription.future.map { sn =>
      f(sn)
      ()
    }
  }

  def onSubscribe()(implicit ec: ExecutionContext): Unit = {
    forSubscription { sn =>
      sn.sr.onSubscribe(sn)
    }
  }

  def onNext(element: T)(implicit ec: ExecutionContext): Unit = {
    forSubscription { sn =>
      sn.onNext(element)
    }
  }

  def onError(t: Throwable)(implicit ec: ExecutionContext): Unit = {
    forSubscription { sn =>
      sn.sr.onError(t)
    }
  }

  def onComplete()(implicit ec: ExecutionContext): Unit = {
    forSubscription { sn =>
      sn.sr.onComplete()
    }
  }
}
