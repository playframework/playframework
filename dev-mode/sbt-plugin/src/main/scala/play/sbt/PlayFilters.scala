/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import sbt._
import sbt.Keys._

/**
 * The plugin that adds Play Filters as a default.
 *
 * To disable this, use sbt's disablePlugins mechanism:
 *
 * {{{
 * lazy val root = project.in(file(".")).enablePlugins(PlayScala).disablePlugins(PlayFilters)
 * }}}
 */
object PlayFilters extends AutoPlugin {
  override def requires = PlayWeb
  override def trigger  = allRequirements

  override def projectSettings =
    Seq(libraryDependencies += PlayImport.filters)
}
