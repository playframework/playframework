addSbtPlugin("com.typesafe.play" % "sbt-scripted-tools" % sys.props("project.version"))
lazy val plugins = (project in file(".")).settings(scalaVersion := "2.12.17")
