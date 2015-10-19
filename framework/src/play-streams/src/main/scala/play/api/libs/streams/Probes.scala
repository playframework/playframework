package play.api.libs.streams

import org.reactivestreams.{ Subscription, Subscriber, Publisher }

/**
 * Probes, for debugging reactive streams.
 */
object Probes {

  private trait Probe {

    def startTime: Long
    def time = System.nanoTime() - startTime

    def probeName: String

    def log[T](method: String, message: String = "")(block: => T) = {
      val threadName = Thread.currentThread().getName
      try {
        println(s"ENTER $probeName.$method at $time in $threadName: $message")
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

    def onError(t: Throwable) = log("onError", s"${t.getClass}: ${t.getMessage}")(subscriber.onError(t))
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

}
