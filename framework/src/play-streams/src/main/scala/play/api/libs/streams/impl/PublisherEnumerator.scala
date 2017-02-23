/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams.impl

import org.reactivestreams._
import play.api.libs.iteratee._
import scala.concurrent.Future

/**
 * Adapts a Publisher to an Enumerator.
 *
 * When an Iteratee is applied to the Enumerator, we adapt the Iteratee into
 * a Subscriber, then subscribe it to the Publisher.
 */
final class PublisherEnumerator[T](pubr: Publisher[T]) extends Enumerator[T] {
  def apply[A](i: Iteratee[T, A]): Future[Iteratee[T, A]] = {
    val subr = new IterateeSubscriber(i)
    pubr.subscribe(subr)
    Future.successful(subr.result)
  }
}
