/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.j

import akka.actor.ActorSystem
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import org.specs2.mutable.Specification
import play.mvc.Http
import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext

object OrderedExecutionContextSpec extends Specification {

  "OrderedExecutionContext" should {

    "execute its tasks in order" in {
      val actorSystem = ActorSystem("OrderedExecutionContextSpec")
      val oec = new OrderedExecutionContext(actorSystem, 64)

      def run(id: Int): (CountDownLatch, AtomicInteger) = {
        val hec = new HttpExecutionContext(
          Thread.currentThread().getContextClassLoader(),
          new Http.Context(id, null, null, Map.empty.asJava, Map.empty.asJava, Map.empty.asJava),
          oec)
        val numTasks = 2000
        val taskCount = new AtomicInteger()
        val outOfOrderCount = new AtomicInteger()
        val ready = new CountDownLatch(numTasks)
        actorSystem.dispatcher.execute(new Runnable {
          def run() = {
            for (i <- 0 until numTasks) {
              hec.execute(new Runnable {
                def run() = {
                  val order = taskCount.getAndIncrement()
                  if (order != i) outOfOrderCount.incrementAndGet()
                  ready.countDown()
                }
              })
            }
          }
        })
        (ready, outOfOrderCount)
      }
      val results = for (i <- 0 until 100) yield run(i)
      for ((ready, outOfOrderCount) <- results) {
        ready.await(10, SECONDS) must beTrue
        outOfOrderCount.get() must equalTo(0)
      }

      actorSystem.shutdown()
      success
    }

  }

}
