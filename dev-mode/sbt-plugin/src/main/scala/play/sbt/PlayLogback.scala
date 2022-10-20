/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt.Keys._
import sbt._

/**
 * This plugin enables Play Logback
 */
object PlayLogback extends AutoPlugin {
  override def requires = PlayService

  // add this plugin automatically if Play is added.
  override def trigger = AllRequirements

  override def projectSettings = Seq(
    libraryDependencies ++= {
      Seq(PlayImport.logback)
    }
  )
}
