/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification

class PlayGlobalAppSpec extends Specification {

  sequential

  def testApp(allowGlobalApp: Boolean): PlayCoreTestApplication =
    PlayCoreTestApplication(Map("play.allowGlobalApplication" -> allowGlobalApp))

  "play.api.Play" should {
    "start apps with global state enabled" in {
      val app = testApp(true)
      Play.start(app)
      Play.privateMaybeApplication must beSome(app)
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
      Play.privateMaybeApplication must beSome(app2)
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
      Play.privateMaybeApplication must beSome(app2)
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
      Play.privateMaybeApplication must beSome(app1)
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
  }
}
