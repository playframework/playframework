// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
// Add additional resolvers in Build.scala

// Use the Play sbt plugin for Play projects
addSbtPlugin("play" % "sbt-plugin" % "%PLAY_VERSION%")