/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.streams

import akka.stream.scaladsl.Flow
import akka.stream.stage._
import org.reactivestreams.{ Processor, Subscription, Subscriber, Publisher }

/**
 * Probes, for debugging reactive streams.
 */
object Probes {

  private trait Probe {

    def startTime: Long
    def time = System.nanoTime() - startTime

    def probeName: String

    def log[T](method: String, message: String = "", logExtra: => Unit = Unit)(block: => T) = {
      val threadName = Thread.currentThread().getName
      try {
        println(s"ENTER $probeName.$method at $time in $threadName: $message")
        logExtra
        block
      } catch {
        case e: Exception =>
          println(s"CATCH $probeName.$method ${e.getClass}: ${e.getMessage}")
          throw e
      } finally {
        println(s"LEAVE $probeName.$method at $time")
      }
    }
  }

  def publisherProbe[T](name: String, publisher: Publisher[T], messageLogger: T => String = (t: T) => t.toString): Publisher[T] = new Publisher[T] with Probe {
    val probeName = name
    val startTime = System.nanoTime()

    def subscribe(subscriber: Subscriber[_ >: T]) = {
      log("subscribe", subscriber.toString)(publisher.subscribe(subscriberProbe(name, subscriber, messageLogger, startTime)))
    }
  }

  def subscriberProbe[T](name: String, subscriber: Subscriber[_ >: T], messageLogger: T => String = (t: T) => t.toString, start: Long = System.nanoTime()): Subscriber[T] = new Subscriber[T] with Probe {
    val probeName = name
    val startTime = start

    def onError(t: Throwable) = {
      log("onError", s"${t.getClass}: ${t.getMessage}", t.printStackTrace())(subscriber.onError(t))
    }
    def onSubscribe(subscription: Subscription) = log("onSubscribe", subscription.toString)(subscriber.onSubscribe(subscriptionProbe(name, subscription, start)))
    def onComplete() = log("onComplete")(subscriber.onComplete())
    def onNext(t: T) = log("onNext", messageLogger(t))(subscriber.onNext(t))
  }

  def subscriptionProbe(name: String, subscription: Subscription, start: Long = System.nanoTime()): Subscription = new Subscription with Probe {
    val probeName = name
    val startTime = start

    def cancel() = log("cancel")(subscription.cancel())
    def request(n: Long) = log("request", n.toString)(subscription.request(n))
  }

  def processorProbe[In, Out](name: String, processor: Processor[In, Out],
    inLogger: In => String = (in: In) => in.toString, outLogger: Out => String = (out: Out) => out.toString): Processor[In, Out] = {
    val subscriber = subscriberProbe(name + "-in", processor, inLogger)
    val publisher = publisherProbe(name + "-out", processor, outLogger)
    new Processor[In, Out] {
      override def onError(t: Throwable): Unit = subscriber.onError(t)
      override def onSubscribe(s: Subscription): Unit = subscriber.onSubscribe(s)
      override def onComplete(): Unit = subscriber.onComplete()
      override def onNext(t: In): Unit = subscriber.onNext(t)
      override def subscribe(s: Subscriber[_ >: Out]): Unit = publisher.subscribe(s)
    }
  }

  def flowProbe[T](name: String, messageLogger: T => String = (t: T) => t.toString): Flow[T, T, _] = {
    Flow[T].transform(() => new PushPullStage[T, T] with Probe {
      override def startTime: Long = System.nanoTime()
      override def probeName: String = name

      override def onPush(elem: T, ctx: Context[T]) = log("onPush", messageLogger(elem))(ctx.push(elem))
      override def onPull(ctx: Context[T]) = log("onPull")(ctx.pull())
      override def preStart(ctx: LifecycleContext) = log("preStart")(super.preStart(ctx))
      override def onUpstreamFinish(ctx: Context[T]) = log("onUpstreamFinish")(super.onUpstreamFinish(ctx))
      override def onDownstreamFinish(ctx: Context[T]) = log("onDownstreamFinish")(super.onDownstreamFinish(ctx))
      override def onUpstreamFailure(cause: Throwable, ctx: Context[T]) = log("onUpstreamFailure", s"${cause.getClass}: ${cause.getMessage}", cause.printStackTrace())(super.onUpstreamFailure(cause, ctx))
      override def postStop() = log("postStop")(super.postStop())
      override def decide(t: Throwable) = log("decide")(super.decide(t))
      override def restart() = log("restart")(super.restart())
    })
  }
}
