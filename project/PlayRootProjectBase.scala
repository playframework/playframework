/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._

/**
 * Base Plugin for a root project that doesn't get published.
 *
 * - Contains release configuration
 */
object PlayRootProjectBase extends AutoPlugin {
  override def trigger         = noTrigger
  override def requires        = PlayBuildBase && PlaySonatypeBase
  override def projectSettings = PlayNoPublishBase.projectSettings
}
