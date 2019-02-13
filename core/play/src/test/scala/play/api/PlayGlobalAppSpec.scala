/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification

class PlayGlobalAppSpec extends Specification {

  sequential

  def testApp(allowGlobalApp: Boolean): PlayCoreTestApplication =
    PlayCoreTestApplication(Map(
      "play.allowGlobalApplication" -> allowGlobalApp,
      "play.akka.config" -> "akka",
      "play.akka.actor-system" -> "global-app-spec",
      "akka.coordinated-shutdown.phases.actor-system-terminate.timeout" -> "90 second",
      "akka.coordinated-shutdown.exit-jvm" -> "off"
    ))

  "play.api.Play" should {
    "start apps with global state enabled" in {
      val app = testApp(true)
      Play.start(app)
      Play.privateMaybeApplication must beSuccessfulTry.withValue(app)
      Play.stop(app)
      success
    }
    "start apps with global state disabled" in {
      val app = testApp(false)
      Play.start(app)
      Play.privateMaybeApplication must throwA[RuntimeException]
      Play.stop(app)
      success
    }
    "shut down the first app when starting a second app with global state enabled" in {
      val app1 = testApp(true)
      Play.start(app1)
      val app2 = testApp(true)
      Play.start(app2)
      app1.isTerminated must beTrue
      app2.isTerminated must beFalse
      Play.privateMaybeApplication must beSuccessfulTry.withValue(app2)
      Play.current must_== app2
      Play.stop(app1)
      Play.stop(app2)
      success
    }
    "start one app with global state after starting another without global state" in {
      val app1 = testApp(false)
      Play.start(app1)
      val app2 = testApp(true)
      Play.start(app2)
      app1.isTerminated must beFalse
      app2.isTerminated must beFalse
      Play.privateMaybeApplication must beSuccessfulTry.withValue(app2)
      Play.stop(app1)
      Play.stop(app2)
      success
    }
    "start one app without global state after starting another with global state" in {
      val app1 = testApp(true)
      Play.start(app1)
      val app2 = testApp(false)
      Play.start(app2)
      app1.isTerminated must beFalse
      app2.isTerminated must beFalse
      Play.privateMaybeApplication must beSuccessfulTry.withValue(app1)
      Play.stop(app1)
      Play.stop(app2)
      success
    }
    "start multiple apps with global state disabled" in {
      val app1 = testApp(false)
      Play.start(app1)
      val app2 = testApp(false)
      Play.start(app2)
      app1.isTerminated must beFalse
      app2.isTerminated must beFalse
      Play.privateMaybeApplication must throwA[RuntimeException]
      Play.stop(app1)
      Play.stop(app2)
      success
    }
    "should stop an app with global state disabled" in {
      val app = testApp(false)
      Play.start(app)
      Play.privateMaybeApplication must throwA[RuntimeException]

      Play.stop(app)
      app.isTerminated must beTrue
    }
    "should unset current app when stopping with global state enabled" in {
      val app = testApp(true)
      Play.start(app)
      Play.privateMaybeApplication must beSuccessfulTry.withValue(app)

      Play.stop(app)
      app.isTerminated must beTrue
      Play.privateMaybeApplication must throwA[RuntimeException]
    }
  }
}
