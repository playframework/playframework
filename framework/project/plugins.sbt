logLevel := Level.Warn

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.5")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "0.6.0")

libraryDependencies <+= sbtVersion { sv =>
  "org.scala-sbt" % "scripted-plugin" % sv
}

