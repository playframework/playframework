/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
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
    // TODO: sbt 1.0
    // For sbt 0.13 we have watched.watchPaths(state) which returns a Seq[sbt.File]
    // that we can then transform in a Iterator[better.files.File]. But for sbt 1.0
    // watched.watchSources(state) returns a Seq[sbt.Watched.WatchSource] where
    // WatchSource is a type alias to sbt.internal.io.Source which has a `base`
    // directory and filters, but there is no access to getUnfilteredPaths.
    //
    // A possible current approach here is to copy code from sbt and make it accessible
    // to Play (maybe using play-file-watch), but it is possible that we need to
    // copy a good amount of code just to have access to a "simple" method.
    //
    // So there is now PlaySource which is class declared inside sbt.internal.io
    // to make it possible to access sbt.internal.io.Source methods.
    watched.watchSources(state)
      .map(source => new PlaySource(source))
      .flatMap(_.getFiles)
      .map(f => better.files.File(f.toPath))
      .toIterator
  }

  def kill(pid: String) = s"kill $pid".!

  def createAndRunProcess(args: Seq[String]) = args.!

}
