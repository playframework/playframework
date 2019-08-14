//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props("project.version"))
                    scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9")
                   updateOptions := updateOptions.value.withLatestSnapshots(false)
evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))
