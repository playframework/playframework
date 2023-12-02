/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import scala.collection.mutable.HashMap
import scala.jdk.CollectionConverters._

import org.specs2.mutable._
import play.runsupport.RunHook
import play.runsupport.RunHookCompositeThrowable
import play.runsupport.RunHooksRunner

class PlayRunHookSpec extends Specification {
  "PlayRunHook runner" should {
    "provide implicit `run` which passes every hook to a provided function" in {
      val hooks                                    = Seq.fill(3)(new PlayRunHook {})
      val executedHooks: HashMap[RunHook, Boolean] = HashMap.empty

      RunHooksRunner.run(hooks.asJava, hook => executedHooks += ((hook, true)))

      (executedHooks.size must be).equalTo(3)
    }

    "re-throw an exception on single hook failure" in {
      val executedHooks: HashMap[RunHook, Boolean] = HashMap.empty
      class HookMockException extends Throwable

      val hooks = Seq.fill(3)(new PlayRunHook {
        executedHooks += ((this, true))
      }) :+ new PlayRunHook {
        override def beforeStarted(): Unit = throw new HookMockException()
      }

      RunHooksRunner.run(hooks.asJava, _.beforeStarted()) must throwA[RuntimeException].like {
        case re => re.getCause must haveClass[HookMockException]
      }

      (executedHooks.size must be).equalTo(3)
    }

    "combine several thrown exceptions into a RunHookCompositeThrowable" in {
      val executedHooks: HashMap[RunHook, Boolean] = HashMap.empty
      class HookFirstMockException  extends Throwable
      class HookSecondMockException extends Throwable

      def createDummyHooks = new PlayRunHook {
        executedHooks += ((this, true))
      }

      val dummyHooks = Seq.fill(3)(createDummyHooks)

      val firstFailure = new PlayRunHook {
        override def beforeStarted(): Unit = throw new HookFirstMockException()
      }

      val lastFailure = new PlayRunHook {
        override def beforeStarted(): Unit = throw new HookSecondMockException()
      }

      val hooks = firstFailure +: dummyHooks :+ lastFailure

      RunHooksRunner.run(hooks.asJava, _.beforeStarted()) must throwA[RunHookCompositeThrowable].like {
        case e: Throwable =>
          e.getMessage must contain("HookFirstMockException")
          e.getMessage must contain("HookSecondMockException")
          e.getMessage must not contain "HookThirdMockException"
      }

      (executedHooks.size must be).equalTo(3)
    }
  }
}
