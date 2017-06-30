/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt.{ AutoPlugin, PluginTrigger, Plugins }

import sbtwhitesource.WhiteSourcePlugin
import sbtwhitesource.WhiteSourcePlugin.autoImport._

/**
 * Configures sbt-whitesource.
 */
object PlayWhitesourcePlugin extends AutoPlugin {

  override def requires: Plugins = WhiteSourcePlugin

  override def trigger: PluginTrigger = allRequirements

  override lazy val projectSettings = Seq(
    whitesourceProduct := "Lightbend Reactive Platform",
    whitesourceAggregateProjectName := "playframework-2.5-previous",
    whitesourceAggregateProjectToken := "5465438f-9ba6-494f-bad0-939b1b260e00"
  )

}
