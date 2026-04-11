/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

import scala.sys.process.Process

import sbt._
import sbt.Keys._

object DocumentationCommands {
  private val javafmtWrapperProp = "play.javafmt.wrapper"

  private val javafmtExports =
    Seq("api", "code", "file", "parser", "tree", "util").map { x =>
      s"-J--add-opens=jdk.compiler/com.sun.tools.javac.${x}=ALL-UNNAMED"
    }

  private def javafmtCommand(name: String, delegatedCommand: String): Command =
    Command.command(
      name,
      Help.more(
        name,
        s"Runs $delegatedCommand in a fresh sbt JVM with the required jdk.compiler module-opening flags"
      )
    ) { state =>
      if (sys.props.get(javafmtWrapperProp).contains("true")) {
        delegatedCommand :: state
      } else {
        val extracted = Project.extract(state)
        val base      = extracted.get(ThisBuild / baseDirectory)
        val sbtArgs   = Seq("sbt", s"-D$javafmtWrapperProp=true") ++ javafmtExports ++ Seq(name)
        val exitCode  = Process(sbtArgs, base).!
        if (exitCode == 0) state else state.fail
      }
    }

  val javafmt         = javafmtCommand("javafmt", "javafmt")
  val javafmtCheck    = javafmtCommand("javafmtCheck", "javafmtCheck")
  val javafmtAll      = javafmtCommand("javafmtAll", "all javafmtAll")
  val javafmtCheckAll = javafmtCommand("javafmtCheckAll", "all javafmtCheckAll")
}
