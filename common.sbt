/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

addCommandAlias(
  "formatCode",
  List(
    "headerCreateAll",
    "scalafmtSbt",
    "scalafmtAll",
    "javafmtAll",
  ).mkString(";") + sys.props.get("sbt_formatCode").map(";" + _).getOrElse("")
)

addCommandAlias(
  "validateCode",
  List(
    "headerCheckAll",
    "scalafmtSbtCheck",
    "scalafmtCheckAll",
    "javafmtCheckAll",
  ).mkString(";") + sys.props.get("sbt_validateCode").map(";" + _).getOrElse("")
)
