// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

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
