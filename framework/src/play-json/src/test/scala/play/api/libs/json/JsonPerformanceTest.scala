package play.api.libs.json

import concurrent.{Await, Future, ExecutionContext}
import java.util.concurrent.Executors
import concurrent.duration.Duration

/**
 * Performance test for JsValue serialization and deserialization.
 *
 * Very crude, but does the job.  Easiest way to run this is in SBT:
 *
 * test:run-main play.api.libs.json.JsonPerformanceTest
 */
object JsonPerformanceTest extends App {

  println("Running serialization test...")

  println("Serialization run 1: " + testSerialization() + "ms")
  println("Serialization run 2: " + testSerialization() + "ms")
  println("Serialization run 3: " + testSerialization() + "ms")

  println("Deserialization run 1: " + testDeserialization() + "ms")
  println("Deserialization run 2: " + testDeserialization() + "ms")
  println("Deserialization run 3: " + testDeserialization() + "ms")

  lazy val jsvalue = Json.obj(
    "f1" -> Json.obj(
      "f1" -> "string",
      "f2" -> "string",
      "f3" -> "string",
      "f4" -> 10,
      "f5" -> Json.arr(
        "string",
        "string",
        "string",
        "string",
        "string"
      ),
      "f6" -> Json.obj(
        "f1" -> 10,
        "f2" -> 20,
        "f3" -> 30,
        "f4" -> "string"
      )

    ),
    "f2" -> "string",
    "f3" -> "string",
    "f4" -> 10,
    "f5" -> true,
    "f6" -> false,
    "f7" -> Json.arr(1, 2, 3, 4, 5, 6)
  )

  lazy val json = Json.stringify(jsvalue)

  def testSerialization(times: Int = 10000000, threads: Int = 100): Long = {
    runTest(times, threads) {
      Json.stringify(jsvalue)
    }
  }

  def testDeserialization(times: Int = 1000000, threads: Int = 100): Long = {
    runTest(times, threads) {
      Json.parse(json)
    }
  }

  def runTest(times: Int, threads: Int)(test: => Unit): Long = {
    val timesPerThread = times / threads

    val executor = Executors.newFixedThreadPool(threads)
    try {
      val context = ExecutionContext.fromExecutor(executor)

      val start = System.currentTimeMillis()

      import ExecutionContext.Implicits.global
      Await.ready(Future.sequence(List.range(0, threads).map { t =>
        Future {
          for (i <- 0 to timesPerThread) {
            test
          }
        }(context)
      }), Duration.Inf)
      System.currentTimeMillis() - start
    } finally {
      executor.shutdownNow()
    }
  }

}
