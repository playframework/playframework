// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// NOTE: !!! THIS IS A COPY !!!                                                                                //
// To edit this file use the main version in https://github.com/playframework/.github/blob/main/sbt/common.sbt //
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////

/**
 * If you need extra commands to format source code or other documents, add following line to your `build.sbt`
 * {{{
 *   val _ = sys.props += ("sbt_formatCode" -> List("<command1>", "<command2>",...).mkString(";"))
 * }}}
 */
addCommandAlias(
  "formatCode",
  List(
    "headerCreateAll",
    "scalafmtSbt",
    "scalafmtAll",
    "javafmtAll"
  ).mkString(";") + sys.props.get("sbt_formatCode").map(";" + _).getOrElse("")
)

/**
 * If you need extra commands to validate source code or other documents, add following line to your `build.sbt`
 * {{{
 *   val _ = sys.props += ("sbt_validateCode" -> List("<command1>", "<command2>",...).mkString(";"))
 * }}}
 */
addCommandAlias(
  "validateCode",
  List(
    "headerCheckAll",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "javafmtCheckAll"
  ).mkString(";") + sys.props.get("sbt_validateCode").map(";" + _).getOrElse("")
)
