// Copyright (C) Lightbend Inc. <https://www.lightbend.com>

// Comment to get more information during initialization
logLevel := Level.Warn

lazy val plugins = (project in file(".")).dependsOn(playDocsPlugin)

lazy val playDocsPlugin = ProjectRef(Path.fileProperty("user.dir").getParentFile, "Play-Docs-Sbt-Plugin")

// Required for Production.md
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

// Add headers to example sources
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.6.5")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.7.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.0.7")

// Required for Tutorial
addSbtPlugin("com.typesafe.play" % "sbt-twirl" % "1.6.0-M1") // sync with project/plugins.sbt

// Required for IDE docs
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "5.2.4")
