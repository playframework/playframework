// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#debian
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DebianPlugin)

Linux / maintainer := "First Lastname <first.last@example.com>"

Linux / packageSummary := "My custom package summary"

packageDescription := "My longer package description"
//#debian
