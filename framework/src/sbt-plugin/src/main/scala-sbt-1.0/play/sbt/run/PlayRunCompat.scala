/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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

  def watchCountinously(state: State): Option[Watched] = {
    // If we have Watched.ContinuousEventMonitor attribute and its state.count
    // is > 0 then we assume we're in ~ run mode
    state.get(Watched.ContinuousEventMonitor)
      .map(_.state())
      .filter(_.count > 0)
      .flatMap(_ => state.get(Watched.Configuration))
  }
}
