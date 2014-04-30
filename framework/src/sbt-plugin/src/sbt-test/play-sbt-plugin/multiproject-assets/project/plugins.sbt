// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

// Needed while we're on a snapshot of SBT idea plugin
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0-M2a")


// FIXME: override provided here to fix a problem related to jdk6:
// https://github.com/apigee/trireme/pull/51
// Remove this dependency on Trireme once the PR is merged and 0.7.3 is released
// (will also require an updated js-engine of course.
libraryDependencies += "io.apigee.trireme" % "trireme-core" % "0.7.3-play23M1"