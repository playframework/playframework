/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt.run

import sbt._
import play.dev.filewatch.{ SourceModificationWatch => PlaySourceModificationWatch }

import scala.collection.JavaConverters._

/**
 * Fix compatibility issues for PlayRun. This is the version compatible with sbt 0.13.
 */
private[run] trait PlayRunCompat {

  def sleepForPoolDelay = Thread.sleep(Watched.PollDelayMillis)

  def getPollInterval(watched: Watched): Int = watched.pollInterval

  def getSourcesFinder(watched: Watched, state: State): PlaySourceModificationWatch.PathFinder = () => watched.watchPaths(state).map(f => better.files.File(f.toURI)).toIterator

  def kill(pid: String) = s"kill $pid".!

  def createAndRunProcess(args: Seq[String]) = {
    val builder = new java.lang.ProcessBuilder(args.asJava)
    Process(builder).!
  }
}
