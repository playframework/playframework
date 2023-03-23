// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

name := "assets-module-sample"

version := "1.0-SNAPSHOT"

scalaVersion  := ScriptedTools.scalaVersionFromJavaProperties()
updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

Assets / LessKeys.less / includeFilter := "*.less"

Assets / LessKeys.less / excludeFilter := new PatternFilter("""[_].*\.less""".r.pattern)
