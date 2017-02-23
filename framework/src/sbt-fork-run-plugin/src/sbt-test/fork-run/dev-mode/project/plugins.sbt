//
// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
//

addSbtPlugin("com.typesafe.play" % "sbt-fork-run-plugin" % playVersion)

addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "1.0.0")

// get the play version from a system property or otherwise the run.properties file (for sbt server)
def playVersion: String = {
  sys.props.get("project.version") orElse {
    val properties = new java.util.Properties
    sbt.IO.load(properties, file("run.properties"))
    Option(properties.getProperty("project.version"))
  } getOrElse {
    sys.error("No play version specified")
  }
}
