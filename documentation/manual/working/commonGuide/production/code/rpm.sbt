//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#rpm
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, RpmPlugin)

Linux / maintainer := "First Lastname <first.last@example.com>"

Linux / packageSummary := "My custom package summary"

packageDescription := "My longer package description"

rpmRelease := "1"

rpmVendor := "example.com"

rpmUrl := Some("http://github.com/example/server")

rpmLicense := Some("Apache v2")
//#rpm
