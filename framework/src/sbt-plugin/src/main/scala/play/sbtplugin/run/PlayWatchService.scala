/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.sbtplugin.run

import play.runsupport.{ PlayWatchService => PWS, PlayWatcher => PW }
import sbt._
import scala.util.Try

object PlayWatchService {

  @deprecated("use play.runsupport.PlayWatchService.default", "2.3.5")
  def default(targetDirectory: File, pollDelayMillis: Int, logger: Logger): PlayWatchService =
    PWS.default(targetDirectory, pollDelayMillis, logger)

  @deprecated("use play.runsupport.PlayWatchService.jnotify", "2.3.5")
  def jnotify(targetDirectory: File): PlayWatchService =
    PWS.jnotify(targetDirectory)

  @deprecated("use play.runsupport.PlayWatchService.jdk7", "2.3.5")
  def jdk7(logger: Logger): PlayWatchService =
    PWS.jdk7(logger)

  @deprecated("use play.runsupport.PlayWatchService.sbt", "2.3.5")
  def sbt(pollDelayMillis: Int): PlayWatchService =
    PWS.sbt(pollDelayMillis)

  @deprecated("use play.runsupport.PlayWatchService.optional", "2.3.5")
  def optional(watchService: Try[PlayWatchService]): PlayWatchService =
    PWS.optional(watchService)
}
