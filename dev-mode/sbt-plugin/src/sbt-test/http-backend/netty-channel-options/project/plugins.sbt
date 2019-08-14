//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("project.version"))
updateOptions := updateOptions.value.withLatestSnapshots(false)
 scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9")
