// Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

lazy val plugins = (project in file(".")).dependsOn(playDocsPlugin)

lazy val playDocsPlugin = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", "Play-Docs-SBT-Plugin")

// Required for Production.md
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")