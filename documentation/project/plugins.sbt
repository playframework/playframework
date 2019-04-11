// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>

// Comment to get more information during initialization
logLevel := Level.Warn

lazy val plugins = (project in file(".")).dependsOn(playDocsPlugin)

lazy val playDocsPlugin = ProjectRef(Path.fileProperty("user.dir").getParentFile, "Play-Docs-Sbt-Plugin")

// Required for Production.md
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

// Required for PlayEnhancer.md
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "1.2.2")

// Add headers to example sources
addSbtPlugin("de.heikoseeberger" % "sbt-header"         % "5.2.0")
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % "0.4.3")

// Required for Tutorial
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.4.1")
