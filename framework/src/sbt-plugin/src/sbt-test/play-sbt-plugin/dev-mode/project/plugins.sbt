//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("project.version"))

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.1.2")

// We need this to test JNotify
libraryDependencies += "com.lightbend.play" % "jnotify" % "0.94-play-2"

unmanagedSourceDirectories in Compile += baseDirectory.value.getParentFile / "project" / s"scala-sbt-${sbtBinaryVersion.value}"