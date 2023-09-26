/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt.AutoPlugin

/**
 * Base Plugin for Play SBT libraries.
 *
 * - Publishes to sonatype
 */
object PlaySbtLibraryBase extends AutoPlugin {

  override def trigger  = noTrigger
  override def requires = PlayBuildBase && PlaySbtBuildBase && PlaySonatypeBase

}
