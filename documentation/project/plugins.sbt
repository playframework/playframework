// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>

// Comment to get more information during initialization
logLevel := Level.Warn

lazy val plugins = (project in file(".")).dependsOn(playDocsPlugin)

lazy val playDocsPlugin = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", "Play-Docs-SBT-Plugin")

// Required for Production.md
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

// Required for PlayEnhancer.md
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.1.0")

// Add headers to example sources
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.0.0")