//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

// Depend on hard coded play-ws version, since we don't need/want the latest, and the system
// properties from this build interfere with the system properties from Play's build.  This
// can be updated to something else when needed, but doesn't need to be.
libraryDependencies += "com.typesafe.play" %% "play-ws" % "2.4.0"

// Add a resources directory, so that we can include a logback configuration, otherwise we
// get debug logs for everything which is huge.
resourceDirectories in Compile += baseDirectory.value / "resources"
resources in Compile ++= (baseDirectory.value / "resources").***.get
