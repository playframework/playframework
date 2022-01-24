//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

name := "assets-module-sample"

version := "1.0-SNAPSHOT"

                    scalaVersion := sys.props("scala.version")
                   updateOptions := updateOptions.value.withLatestSnapshots(false)
update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

Assets / LessKeys.less / includeFilter := "*.less"

Assets / LessKeys.less / excludeFilter := new PatternFilter("""[_].*\.less""".r.pattern)
