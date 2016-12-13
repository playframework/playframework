// Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>

buildInfoSettings

sourceGenerators in Compile += Def.task(buildInfo.value).taskValue

val sbtNativePackagerVersion = "1.1.1"
val sbtTwirlVersion = sys.props.getOrElse("twirl.version", "1.3.0")

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackagerVersion,
  "sbtTwirlVersion" -> sbtTwirlVersion
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play" % "interplay" % sys.props.getOrElse("interplay.version", "1.3.1"))
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % sbtTwirlVersion)
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.12")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.16")

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value,
  "org.webjars" % "webjars-locator-core" % "0.26"
)

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.eed3si9n" % "sbt-doge" % "0.1.5")
