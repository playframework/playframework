// Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>

buildInfoSettings

sourceGenerators in Compile += Def.task(buildInfo.value).taskValue

val Versions = new {
  val sbtNativePackager = "1.1.1"
  val mima = "0.1.12"
  val sbtScalariform = "1.6.0"
  val sbtJmh = "0.2.20"
  val sbtDoge = "0.1.5"
  val webjarsLocatorCore = "0.26"
  val sbtTwirl: String = sys.props.getOrElse("twirl.version", "1.3.0")
  val interplay: String = sys.props.getOrElse("interplay.version", "1.3.4")
}

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> Versions.sbtNativePackager,
  "sbtTwirlVersion" -> Versions.sbtTwirl
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play" % "interplay" % Versions.interplay)
addSbtPlugin("com.typesafe.sbt" % "sbt-twirl" % Versions.sbtTwirl)
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % Versions.mima)
addSbtPlugin("org.scalariform" % "sbt-scalariform" % Versions.sbtScalariform)
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % Versions.sbtJmh)

libraryDependencies ++= Seq(
  "org.scala-sbt" % "scripted-plugin" % sbtVersion.value,
  "org.webjars" % "webjars-locator-core" % Versions.webjarsLocatorCore
)

resolvers += Resolver.typesafeRepo("releases")

addSbtPlugin("com.eed3si9n" % "sbt-doge" % Versions.sbtDoge)
