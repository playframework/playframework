/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification

class PlayGlobalAppSpec extends Specification {
  sequential

  def testApp(): PlayCoreTestApplication =
    PlayCoreTestApplication(
      Map(
        "play.akka.actor-system"                                          -> "global-app-spec",
        "akka.coordinated-shutdown.phases.actor-system-terminate.timeout" -> "90 second",
        "akka.coordinated-shutdown.exit-jvm"                              -> "off"
      )
    )

  "play.api.Play" should {
    "start apps with global state disabled" in {
      val app = testApp()
      Play.start(app)
      Play.stop(app)
      success
    }
    "start multiple apps with global state disabled" in {
      val app1 = testApp()
      Play.start(app1)
      val app2 = testApp()
      Play.start(app2)
      app1.isTerminated must beFalse
      app2.isTerminated must beFalse
      Play.stop(app1)
      Play.stop(app2)
      success
    }
    "should stop an app with global state disabled" in {
      val app = testApp()
      Play.start(app)

      Play.stop(app)
      app.isTerminated must beTrue
    }
  }
}
