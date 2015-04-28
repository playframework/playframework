// Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>

buildInfoSettings

sourceGenerators in Compile <+= buildInfo

val sbtNativePackagerVersion = "1.0.0"
val sbtTwirlVersion = "1.0.4"

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackagerVersion,
  "sbtTwirlVersion" -> sbtTwirlVersion
)

addSbtPlugin("com.eed3si9n" % "bintray-sbt" % "0.3.0-a1934a5457f882053b08cbdab5fd4eb3c2d1285d")
resolvers += Resolver.url("bintray-eed3si9n-sbt-plugins", url("https://dl.bintray.com/eed3si9n/sbt-plugins/"))(Resolver.ivyStylePatterns)

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "0.2.2")

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
