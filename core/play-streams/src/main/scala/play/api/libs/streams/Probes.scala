/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.streams

import org.apache.pekko.stream._
import org.apache.pekko.stream.scaladsl.Flow
import org.apache.pekko.stream.stage._
import org.reactivestreams.Processor
import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription

/**
 * Probes, for debugging reactive streams.
 */
object Probes {
  private trait Probe {
    def startTime: Long
    def time = System.nanoTime() - startTime

    def probeName: String

    def log[T](method: String, message: String = "", logExtra: => Unit = ())(block: => T) = {
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

  def publisherProbe[T](
      name: String,
      publisher: Publisher[T],
      messageLogger: T => String = (t: T) => t.toString
  ): Publisher[T] = new Publisher[T] with Probe {
    val probeName = name
    val startTime = System.nanoTime()

    def subscribe(subscriber: Subscriber[_ >: T]) = {
      log("subscribe", subscriber.toString)(
        publisher.subscribe(subscriberProbe(name, subscriber, messageLogger, startTime))
      )
    }
  }

  def subscriberProbe[T](
      name: String,
      subscriber: Subscriber[_ >: T],
      messageLogger: T => String = (t: T) => t.toString,
      start: Long = System.nanoTime()
  ): Subscriber[T] = new Subscriber[T] with Probe {
    val probeName = name
    val startTime = start

    def onError(t: Throwable) = {
      log("onError", s"${t.getClass}: ${t.getMessage}", t.printStackTrace())(subscriber.onError(t))
    }
    def onSubscribe(subscription: Subscription) =
      log("onSubscribe", subscription.toString)(subscriber.onSubscribe(subscriptionProbe(name, subscription, start)))
    def onComplete() = log("onComplete")(subscriber.onComplete())
    def onNext(t: T) = log("onNext", messageLogger(t))(subscriber.onNext(t))
  }

  def subscriptionProbe(name: String, subscription: Subscription, start: Long = System.nanoTime()): Subscription =
    new Subscription with Probe {
      val probeName = name
      val startTime = start

      def cancel()         = log("cancel")(subscription.cancel())
      def request(n: Long) = log("request", n.toString)(subscription.request(n))
    }

  def processorProbe[In, Out](
      name: String,
      processor: Processor[In, Out],
      inLogger: In => String = (in: In) => in.toString,
      outLogger: Out => String = (out: Out) => out.toString
  ): Processor[In, Out] = {
    val subscriber = subscriberProbe(name + "-in", processor, inLogger)
    val publisher  = publisherProbe(name + "-out", processor, outLogger)
    new Processor[In, Out] {
      override def onError(t: Throwable): Unit              = subscriber.onError(t)
      override def onSubscribe(s: Subscription): Unit       = subscriber.onSubscribe(s)
      override def onComplete(): Unit                       = subscriber.onComplete()
      override def onNext(t: In): Unit                      = subscriber.onNext(t)
      override def subscribe(s: Subscriber[_ >: Out]): Unit = publisher.subscribe(s)
    }
  }

  def flowProbe[T](name: String, messageLogger: T => String = (t: T) => t.toString): Flow[T, T, _] = {
    Flow[T].via(new GraphStage[FlowShape[T, T]] with Probe {
      val in  = Inlet[T]("Probes.in")
      val out = Outlet[T]("Probes.out")

      override def shape: FlowShape[T, T] = FlowShape.of(in, out)

      override def startTime: Long   = System.nanoTime()
      override def probeName: String = name

      override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
        new GraphStageLogic(shape) with OutHandler with InHandler {
          override def onPush(): Unit = {
            val elem = grab(in)
            log("onPush", messageLogger(elem))(push(out, elem))
          }
          override def onPull(): Unit                             = log("onPull")(pull(in))
          override def preStart(): Unit                           = log("preStart")(super.preStart())
          override def onUpstreamFinish(): Unit                   = log("onUpstreamFinish")(super.onUpstreamFinish())
          override def onDownstreamFinish(cause: Throwable): Unit =
            log("onDownstreamFinish", s"${cause.getClass}: ${cause.getMessage}", cause.printStackTrace()) {
              super.onDownstreamFinish(cause)
            }
          override def onUpstreamFailure(cause: Throwable): Unit =
            log("onUpstreamFailure", s"${cause.getClass}: ${cause.getMessage}", cause.printStackTrace())(
              super.onUpstreamFailure(cause)
            )
          override def postStop(): Unit = log("postStop")(super.postStop())

          setHandlers(in, out, this)
        }
    })
  }
}
