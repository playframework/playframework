/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
object FileWatchServiceInitializer {
  lazy val initialFileWatchService = play.dev.filewatch.FileWatchService.sbt(500)
}