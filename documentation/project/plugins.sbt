// Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>

// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository
resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "play-docs-sbt-plugin" % Option(System.getProperty("play.version")).getOrElse {
  println("[\033[31merror\033[0m] No play.version system property specified.\n[\033[31merror\033[0m] Just use the build script to launch SBT and life will be much easier.")
  System.exit(1)
  throw new RuntimeException("No play version")
})
