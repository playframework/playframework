/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import sbt._
import sbt.Keys._

/**
 * This plugins adds Pekko snapshot repositories when running a nightly build.
 */
object PekkoSnapshotRepositories extends AutoPlugin {
  override def trigger: PluginTrigger = allRequirements

  // This is also copy/pasted in ScriptedTools for scripted tests to also use the snapshot repositories.
  override def projectSettings: Seq[Def.Setting[_]] = {
    // If this is a scheduled GitHub Action
    // https://docs.github.com/en/actions/learn-github-actions/environment-variables
    resolvers ++= sys.env
      .get("GITHUB_EVENT_NAME")
      .filter(_.equalsIgnoreCase("schedule"))
      .map(_ => Resolver.ApacheMavenSnapshotsRepo) // contains pekko(-http) snapshots
      .toSeq
  }
}
