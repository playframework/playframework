package test

import scala.language.reflectiveCalls

import play.api.test._
import play.api.test.Helpers._
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import collection.mutable

import org.specs2.mutable._

class TestHelperSpec extends Specification {

  "a test using the 'running' helper" should {

    "not run while another test is doing the same" in {

      val testsRunning = new AtomicInteger(2)
      val allTestsReady = new CountDownLatch(1)
      val firstTestStarted = new CountDownLatch(1)
      val allTestsFinished = new CountDownLatch(2)

      val testMap = mutable.HashMap("one-more" -> -1, "no-more" -> -1)

      new Thread(new RunningFakeTester(true, testsRunning, testMap, allTestsReady, firstTestStarted, allTestsFinished), "one-more").start()
      new Thread(new RunningFakeTester(false, testsRunning, testMap, allTestsReady, firstTestStarted, allTestsFinished), "no-more").start()

      allTestsReady.countDown()
      allTestsFinished.await()

      testMap("one-more") should be equalTo 1
      testMap("no-more") should be equalTo 0
    }
  }

  class RunningFakeTester(isFirst: Boolean, testsRunning: AtomicInteger, testMap: mutable.HashMap[String, Int], allTestsReady: CountDownLatch, firstTestStarted: CountDownLatch, allTestsFinished: CountDownLatch) extends Runnable {
    def run() = {
      val name = Thread.currentThread.getName()

      allTestsReady.await()
      if (!isFirst) firstTestStarted.await()

      running(FakeApplication()) {
        if (isFirst) {
          firstTestStarted.countDown()
          Thread.sleep(2)
        }
        testMap += (Thread.currentThread.getName() -> testsRunning.decrementAndGet())
        allTestsFinished.countDown()
      }
    }
  }
}
