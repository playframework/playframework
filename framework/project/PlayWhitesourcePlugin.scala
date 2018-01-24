/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import sbt._
import sbt.Keys._
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
    whitesourceAggregateProjectName := "play-framework-2.5-" + {
      if (isSnapshot.value) "-snapshot" else "-previous"
    }
  )

}
