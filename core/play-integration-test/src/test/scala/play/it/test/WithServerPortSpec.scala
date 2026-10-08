/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.it.test

import org.specs2.execute.Failure
import org.specs2.execute.FailureException
import play.api.test.PlaySpecification
import play.api.test.WithBrowser
import play.api.test.WithServer

/**
 * Tests that the specs2 [[WithServer]] and [[WithBrowser]] scopes reset their port after an example.
 */
class WithServerPortSpec extends PlaySpecification {
  "WithServer" should {
    "reset the port after a successful example" in {
      var scope: WithServer = null
      var portWhileRunning  = 0
      new WithServer(port = 0) {
        override def running() = {
          scope = this
          portWhileRunning = this.port
        }
      }
      portWhileRunning must be_>(0)
      scope.port must_== 0
    }

    "reset the port after a failing example" in {
      var scope: WithServer = null
      var portWhileRunning  = 0
      new WithServer(port = 0) {
        override def running(): Unit = {
          scope = this
          portWhileRunning = this.port
          throw FailureException(Failure("failing example"))
        }
      } must throwA[FailureException]
      portWhileRunning must be_>(0)
      scope.port must_== 0
    }
  }

  "WithBrowser" should {
    "reset the port after a successful example" in {
      var scope: WithBrowser[?] = null
      var portWhileRunning      = 0
      new WithBrowser(port = 0) {
        override def running() = {
          scope = this
          portWhileRunning = this.port
        }
      }
      portWhileRunning must be_>(0)
      scope.port must_== 0
    }

    "reset the port after a failing example" in {
      var scope: WithBrowser[?] = null
      var portWhileRunning      = 0
      new WithBrowser(port = 0) {
        override def running(): Unit = {
          scope = this
          portWhileRunning = this.port
          throw FailureException(Failure("failing example"))
        }
      } must throwA[FailureException]
      portWhileRunning must be_>(0)
      scope.port must_== 0
    }
  }
}
