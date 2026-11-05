/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package sbt

import scala.collection.mutable

import org.specs2.mutable.Specification
import play.core.BuildLink
import play.runsupport.DevServer

class PlayContinuousRunSpec extends Specification {
  import PlayContinuousRun.*

  private val root  = ProjectRef(java.net.URI.create("file:/build/"), "root")
  private val other = root.copy(project = "other")

  private val watching: String => Boolean = _ => true
  private val stopped: String => Boolean  = _ => false

  "ContinuousRun ownership" should {
    "start when no server is owned" in {
      decide(None, root, "a", Some(new Object), ownerIsWatching = true) must_== Start
    }

    "reuse only the identical watch on the same channel" in {
      val watch = new Object
      decide(Some(Owner(root, "a", watch)), root, "a", Some(watch), ownerIsWatching = true) must_== Reuse
    }

    "restart for distinct watch sets with equal contents" in {
      val previous = mutable.Set("source")
      val next     = mutable.Set("source")
      previous must_== next
      decide(Some(Owner(root, "a", previous)), root, "a", Some(next), ownerIsWatching = true) must_== Restart
    }

    "reject plain run while its channel is still watching" in {
      decide(Some(Owner(root, "a", new Object)), root, "a", None, ownerIsWatching = true) must_== Reject
    }

    "reject another channel's plain run while the owner is watching" in {
      decide(Some(Owner(root, "a", new Object)), root, "b", None, ownerIsWatching = true) must_== Reject
    }

    "reject another channel's watch even if it presents the same identity" in {
      val watch = new Object
      decide(Some(Owner(root, "a", watch)), root, "b", Some(watch), ownerIsWatching = true) must_== Reject
    }

    "recover a stopped owner's server before plain run" in {
      decide(Some(Owner(root, "a", new Object)), root, "b", None, ownerIsWatching = false) must_== Restart
    }

    "recover a stopped owner's server before another channel's watch" in {
      decide(Some(Owner(root, "a", new Object)), root, "b", Some(new Object), ownerIsWatching = false) must_== Restart
    }
  }

  "ContinuousRun project ownership" should {
    "reject another project in the same watch instead of reusing the first project's server" in {
      val watch = new Object
      decide(Some(Owner(root, "a", watch)), other, "a", Some(watch), ownerIsWatching = true) must_== Reject
    }

    "restart for another project when the same channel starts a new watch" in {
      decide(Some(Owner(root, "a", new Object)), other, "a", Some(new Object), ownerIsWatching = true) must_== Restart
    }

    "distinguish projects with the same name in different builds" in {
      val watch      = new Object
      val otherBuild = root.copy(build = java.net.URI.create("file:/other-build/"))
      decide(Some(Owner(root, "a", watch)), otherBuild, "a", Some(watch), ownerIsWatching = true) must_== Reject
    }

    "reject another project's server factory within the same watch" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      val watch  = new Object
      run.start(root, "a", watch, watching, unexpectedWarning)(server)
      run.start(other, "a", watch, watching, unexpectedWarning)(sys.error("Created the wrong project's server")) must
        throwA[MessageOnlyException](".*project root already owns it in this watch, so project other cannot start.*")
      run.compile(root, "a")
      server.compilations must_== 1
      server.closes must_== 0
    }

    "ignore another project's compile and termination on the same channel" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      run.start(root, "a", new Object, watching, unexpectedWarning)(server)
      run.compile(other, "a")
      run.stop(other, "a")
      server.compilations must_== 0
      server.closes must_== 0
      run.stop(root, "a")
      server.closes must_== 1
    }

    "recover a stale server from another project before plain run" in {
      val run      = new PlayContinuousRun
      val server   = new TestServer
      val warnings = mutable.Buffer.empty[String]
      run.start(root, "a", new Object, watching, warnings += _)(server)
      run.beforeRun(other, "a", None, stopped, warnings += _)
      server.closes must_== 1
      warnings.size must_== 1
    }
  }

  "ContinuousRun lifecycle" should {
    "reuse a server without evaluating another server factory" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      val watch  = new Object
      run.start(root, "a", watch, watching, unexpectedWarning)(server) must beTrue
      run.start(root, "a", watch, watching, unexpectedWarning)(sys.error("Created a redundant server")) must beFalse
      run.compile(root, "a")
      server.compilations must_== 1
      server.closes must_== 0
    }

    "leave an active owner's server operational when another watch is rejected" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      run.start(root, "a", new Object, watching, unexpectedWarning)(server)
      run.start(root, "b", new Object, watching, unexpectedWarning)(sys.error("Created a rejected server")) must
        throwA[MessageOnlyException](".*channel a.*")
      run.stop(root, "b")
      run.compile(root, "a")
      server.closes must_== 0
      server.compilations must_== 1
    }

    "leave an active owner's server untouched when plain run is rejected" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      run.start(root, "a", new Object, watching, unexpectedWarning)(server)
      run.beforeRun(root, "b", None, watching, unexpectedWarning) must throwA[MessageOnlyException]
      server.closes must_== 0
    }

    "close the previous session exactly once when its channel starts a new watch" in {
      val run      = new PlayContinuousRun
      val previous = new TestServer
      val next     = new TestServer
      val warnings = mutable.Buffer.empty[String]
      run.start(root, "a", new Object, watching, warnings += _)(previous)
      run.start(root, "a", new Object, watching, warnings += _)(next) must beTrue
      previous.closes must_== 1
      warnings.size must_== 1
      run.stop(root, "a")
      run.stop(root, "a")
      previous.closes must_== 1
      next.closes must_== 1
    }

    "recover a disconnected owner's server" in {
      val run      = new PlayContinuousRun
      val previous = new TestServer
      val next     = new TestServer
      val warnings = mutable.Buffer.empty[String]
      run.start(root, "disconnected", new Object, watching, warnings += _)(previous)
      run.start(root, "b", new Object, stopped, warnings += _)(next) must beTrue
      previous.closes must_== 1
      next.closes must_== 0
      warnings.size must_== 1
    }

    "recover before plain run without repeating cleanup or warnings" in {
      val run      = new PlayContinuousRun
      val previous = new TestServer
      val warnings = mutable.Buffer.empty[String]
      run.start(root, "a", new Object, watching, warnings += _)(previous)
      run.beforeRun(root, "a", None, stopped, warnings += _)
      run.beforeRun(root, "b", None, stopped, warnings += _)
      run.stop(root, "a")
      previous.closes must_== 1
      warnings.size must_== 1
    }

    "recover a stale server before a blocking run in a later watch on the same channel" in {
      val run      = new PlayContinuousRun
      val previous = new TestServer
      val warnings = mutable.Buffer.empty[String]
      run.start(root, "a", new Object, watching, warnings += _)(previous)
      run.beforeRun(root, "a", Some(new Object), watching, warnings += _)
      previous.closes must_== 1
      warnings.size must_== 1
    }

    "reject a blocking run inside the watch that owns the server" in {
      val run    = new PlayContinuousRun
      val server = new TestServer
      val watch  = new Object
      run.start(root, "a", watch, watching, unexpectedWarning)(server)
      run.beforeRun(root, "a", Some(watch), watching, unexpectedWarning) must
        throwA[MessageOnlyException](".*already owns it in this watch.*")
      server.closes must_== 0
    }

    "retry a failed server start without retaining an owner" in {
      val run = new PlayContinuousRun
      run.start(root, "a", new Object, watching, unexpectedWarning)(sys.error("Failed start")) must
        throwA[RuntimeException]("Failed start")
      run.start(root, "b", new Object, watching, unexpectedWarning)(new TestServer) must beTrue
    }
  }

  private def unexpectedWarning(message: String): Unit = sys.error(message)

  private class TestServer extends DevServer {
    var closes                          = 0
    var compilations                    = 0
    override def close(): Unit          = { closes += 1 }
    override def compile(): Unit        = { compilations += 1 }
    override def reload(): Unit         = ()
    override def buildLink(): BuildLink = null
  }
}
