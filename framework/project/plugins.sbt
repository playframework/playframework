// Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

val sbtNativePackagerVersion = "1.0.0-RC1"
val sbtTwirlVersion = "1.0.4"

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackagerVersion,
  "sbtTwirlVersion" -> sbtTwirlVersion
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play" % "interplay" % "0.1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % sbtTwirlVersion)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.6")

addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % sbtNativePackagerVersion)

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value,
  "org.webjars" % "webjars-locator" % "0.19"
)

// override scalariform version to get some fixes

resolvers += Resolver.typesafeRepo("maven-releases")

libraryDependencies += "org.scalariform" %% "scalariform" % "0.1.5-20140822-69e2e30"

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
