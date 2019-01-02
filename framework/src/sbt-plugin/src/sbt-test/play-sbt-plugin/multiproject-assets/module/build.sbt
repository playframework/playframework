//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

name := "assets-module-sample"

version := "1.0-SNAPSHOT"

scalaVersion := sys.props.get("scala.version").getOrElse("2.12.8")

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := new PatternFilter("""[_].*\.less""".r.pattern)
