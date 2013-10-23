// Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % Option(System.getProperty("play.version")).get)

libraryDependencies += "com.typesafe.play" %% "play-doc" % "1.0.3"
