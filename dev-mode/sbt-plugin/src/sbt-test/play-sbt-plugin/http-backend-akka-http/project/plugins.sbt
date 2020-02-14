//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

updateOptions := updateOptions.value.withLatestSnapshots(false)
addSbtPlugin("com.typesafe.play" % "sbt-plugin"         % sys.props("project.version"))
addSbtPlugin("com.typesafe.play" % "sbt-scripted-tools" % sys.props("project.version"))
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")