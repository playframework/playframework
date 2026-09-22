// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(BuildInfoPlugin)

resolvers += Resolver.sonatypeCentralSnapshots

// when updating sbtNativePackager version, be sure to also update the documentation links in
// documentation/manual/working/commonGuide/production/Deploying.md
val sbtNativePackager  = "1.12.0"
val mima               = "1.2.1"
val sbtJavaFormatter   = "0.12.0"
val sbtJmh             = "0.4.8"
val webjarsLocatorCore = "0.59"
val sbtHeader          = "5.11.0"
val scalafmt           = "2.5.6"
val sbtTwirl: String   =
  sys.props.getOrElse("twirl.version", "2.1.0-M9+129-bab5ac69-SNAPSHOT") // sync with documentation/project/plugins.sbt

buildInfoPackage := "playbuildinfo"
buildInfoKeys    := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackager,
  "sbtTwirlVersion"          -> sbtTwirl,
)

logLevel := Level.Warn

scalacOptions += "-deprecation"

addSbtPlugin("org.playframework.twirl" % "sbt-twirl"          % sbtTwirl)
addSbtPlugin("com.typesafe"            % "sbt-mima-plugin"    % mima)
addSbtPlugin("com.github.sbt"          % "sbt-java-formatter" % sbtJavaFormatter)
addSbtPlugin("pl.project13.scala"      % "sbt-jmh"            % sbtJmh)
addSbtPlugin("com.github.sbt"          % "sbt-header"         % sbtHeader)
addSbtPlugin("org.scalameta"           % "sbt-scalafmt"       % scalafmt)
addSbtPlugin("com.github.sbt"          % "sbt-ci-release"     % "1.12.1")

addSbtPlugin("nl.gn0s1s" % "sbt-pekko-version-check" % "0.0.10")

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % webjarsLocatorCore
)
