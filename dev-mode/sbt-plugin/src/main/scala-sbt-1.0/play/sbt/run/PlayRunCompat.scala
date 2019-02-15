/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import sbt.{ State, Watched }
import sbt.internal.io.PlaySource

import play.dev.filewatch.SourceModificationWatch

import scala.sys.process._

/**
 * Fix compatibility issues for PlayRun. This is the version compatible with sbt 1.0.
 */
private[run] trait PlayRunCompat {

  def sleepForPoolDelay = Thread.sleep(Watched.PollDelay.toMillis)

  def getPollInterval(watched: Watched): Int = watched.pollInterval.toMillis.toInt

  def getSourcesFinder(watched: Watched, state: State): SourceModificationWatch.PathFinder = () => {
    watched.watchSources(state)
      .map(source => new PlaySource(source))
      .flatMap(_.getFiles)
      .collect {
        case f if f.exists() => better.files.File(f.toPath)
      }(scala.collection.breakOut)
  }

  def kill(pid: String) = s"kill -15 $pid".!

  def createAndRunProcess(args: Seq[String]) = args.!

  def watchContinuously(state: State, sbtVersion: String): Option[Watched] = {

    // sbt 1.1.5+ uses Watched.ContinuousEventMonitor while watching the file system.
    def watchUsingEvenMonitor = {
      // If we have Watched.ContinuousEventMonitor attribute and its state.count
      // is > 0 then we assume we're in ~ run mode
      state.get(Watched.ContinuousEventMonitor)
        .map(_.state())
        .filter(_.count > 0)
        .flatMap(_ => state.get(Watched.Configuration))
    }

    // sbt 1.1.4 and earlier uses Watched.ContinuousState while watching the file system.
    def watchUsingContinuousState = {
      // If we have both Watched.Configuration and Watched.ContinuousState
      // attributes and if Watched.ContinuousState.count is 1 then we assume
      // we're in ~ run mode
      for {
        watched <- state.get(Watched.Configuration)
        watchState <- state.get(Watched.ContinuousState)
        if watchState.count == 1
      } yield watched
    }

    val _ :: minor :: patch :: Nil = sbtVersion.split("\\.").map(_.toInt).toList

    if (minor >= 2) { // sbt 1.2.x and later
      watchUsingEvenMonitor
    } else if (minor == 1 && patch >= 5) { // sbt 1.1.5+
      watchUsingEvenMonitor
    } else { // sbt 1.1.4 and earlier
      watchUsingContinuousState
    }
  }
}
