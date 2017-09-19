/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.run

import sbt.{ State, Watched }
import play.dev.filewatch.SourceModificationWatch

import scala.sys.process._

/**
 * Fix compatibility issues for PlayRun. This is the version compatible with sbt 1.0.
 */
private[run] trait PlayRunCompat {

  def sleepForPoolDelay = Thread.sleep(Watched.PollDelay.toMillis)

  def getPollInterval(watched: Watched): Int = watched.pollInterval.toMillis.toInt

  def getSourcesFinder(watched: Watched, state: State): SourceModificationWatch.PathFinder = () => {
    //    watched.watchSources(state)
    //      // TODO: sbt 1.0
    //      // Using unfiltered paths may not be what we want.
    //      // Besides that it is private[sbt] so it won't be
    //      // accessible from here.
    //      .flatMap(_.getUnfilteredPaths)
    //      .map(p => better.files.File(f))
    //      .toIterator
    Seq.empty[better.files.File].toIterator
  }

  def kill(pid: String) = s"kill $pid".!

  def createAndRunProcess(args: Seq[String]) = args.!

}
