/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core.server.netty

import org.reactivestreams.{ Processor, Publisher, Subscription, Subscriber }

object SynchronousMappedStreams {

  private class SynchronousContramappedSubscriber[A, B](subscriber: Subscriber[_ >: B], f: A => B) extends Subscriber[A] {
    override def onError(t: Throwable): Unit = subscriber.onError(t)
    override def onSubscribe(s: Subscription): Unit = subscriber.onSubscribe(s)
    override def onComplete(): Unit = subscriber.onComplete()
    override def onNext(a: A): Unit = subscriber.onNext(f(a))
    override def toString = s"SynchronousContramappedSubscriber($subscriber)"
  }

  private class SynchronousMappedPublisher[A, B](publisher: Publisher[A], f: A => B) extends Publisher[B] {
    override def subscribe(s: Subscriber[_ >: B]): Unit =
      publisher.subscribe(new SynchronousContramappedSubscriber[A, B](s, f))
    override def toString = s"SynchronousMappedPublisher($publisher)"
  }

  private class JoinedProcessor[A, B](subscriber: Subscriber[A], publisher: Publisher[B]) extends Processor[A, B] {
    override def onError(t: Throwable): Unit = subscriber.onError(t)
    override def onSubscribe(s: Subscription): Unit = subscriber.onSubscribe(s)
    override def onComplete(): Unit = subscriber.onComplete()
    override def onNext(t: A): Unit = subscriber.onNext(t)
    override def subscribe(s: Subscriber[_ >: B]): Unit = publisher.subscribe(s)
    override def toString = s"JoinedProcessor($subscriber, $publisher)"
  }

  /**
   * Maps a publisher using a synchronous function.
   *
   * This is useful in situations where you want to guarantee that messages produced by the publisher are always
   * handled, but can't guarantee that the subscriber passed to it will always handle them. For example, a
   * publisher that produces Netty `ByteBuf` can't be fed directly into an Akka streams subscriber since Akka streams
   * may drop the message without giving any opportunity to release the `ByteBuf`, this can be used to consume the
   * `ByteBuf` and then release it.
   */
  def map[A, B](publisher: Publisher[A], f: A => B): Publisher[B] =
    new SynchronousMappedPublisher(publisher, f)

  /**
   * Contramaps a subscriber using a synchronous function.
   *
   * This is useful in situations where you want to guarantee that messages that you produce always reach passed to the subscriber are always
   * handled, but can't guarantee that the subscriber being contramapped will always handle them. For example, a
   * subscriber that consumes Netty `ByteBuf` can't subscribe directly to an Akka streams publisher since Akka streams
   * may drop the messages its publishing without giving any opportunity to release the `ByteBuf`, this can be used to
   * to convert some other immutable message to a `ByteBuf` for consumption by the Netty subscriber.
   */
  def contramap[A, B](subscriber: Subscriber[B], f: A => B): Subscriber[A] =
    new SynchronousContramappedSubscriber(subscriber, f)

  /**
   * Does a map and contramap on the processor.
   *
   * @see [[map]] and [[contramap]].
   */
  def transform[A1, B1, A2, B2](processor: Processor[B1, A2], f: A1 => B1, g: A2 => B2): Processor[A1, B2] =
    new JoinedProcessor[A1, B2](contramap(processor, f), map(processor, g))
}
