//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

name := "assets-module-sample"

version := "1.0-SNAPSHOT"

scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.11.7")

includeFilter in (Assets, LessKeys.less) := "*.less"

excludeFilter in (Assets, LessKeys.less) := new PatternFilter("""[_].*\.less""".r.pattern)
