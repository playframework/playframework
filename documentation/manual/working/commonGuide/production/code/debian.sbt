//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#debian
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DebianPlugin)

Linux / maintainer := "First Lastname <first.last@example.com>"

Linux / packageSummary := "My custom package summary"

packageDescription := "My longer package description"
//#debian
