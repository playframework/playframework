// Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>

logLevel := Level.Warn

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % "1.0.2")

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.2.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.7.2")

libraryDependencies <+= sbtVersion { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

libraryDependencies += "org.webjars" % "webjars-locator" % "0.12"

// TODO: remove this workaround for sbt-native-packager pulling in old version of slf4j via jdeb
// https://github.com/sbt/sbt-native-packager/issues/291
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api"       % "1.7.7" force(),
  "org.slf4j" % "slf4j-nop"       % "1.7.7" force(),
  "org.slf4j" % "slf4j-jdk14"     % "1.7.7" force(),
  "org.slf4j" % "jcl-over-slf4j"  % "1.7.7" force()
)
