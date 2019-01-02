//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("project.version"))
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "1.1.2")

unmanagedSourceDirectories in Compile += baseDirectory.value.getParentFile / "project" / s"scala-sbt-${sbtBinaryVersion.value}"
