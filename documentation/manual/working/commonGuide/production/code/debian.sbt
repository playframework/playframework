//
// Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
//

//#debian
lazy val root = (project in file("."))
  .enablePlugins(PlayScala, DebianPlugin)

maintainer in Linux := "First Lastname <first.last@example.com>"

packageSummary in Linux := "My custom package summary"

packageDescription := "My longer package description"
//#debian