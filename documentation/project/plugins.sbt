// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

// Comment to get more information during initialization
logLevel := Level.Warn

lazy val plugins = (project in file(".")).dependsOn(playDocsPlugin)

lazy val playDocsPlugin = ProjectRef(Path.fileProperty("user.dir").getParentFile, "Play-Docs-Sbt-Plugin")

// Required for Production.md
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "2.2.0")

// Add headers to example sources
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.10.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.8.0")
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"       % "2.5.2")

// Required for Tutorial
addSbtPlugin("org.playframework.twirl" % "sbt-twirl" % "2.0.5") // sync with project/plugins.sbt

// Required for IDE docs
addSbtPlugin("com.github.sbt" % "sbt-eclipse" % "6.2.0")
