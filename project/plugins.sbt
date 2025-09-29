// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(BuildInfoPlugin)

addDependencyTreePlugin

// when updating sbtNativePackager version, be sure to also update the documentation links in
// documentation/manual/working/commonGuide/production/Deploying.md
val sbtNativePackager  = "1.11.3"
val mima               = "1.1.4"
val sbtJavaFormatter   = "0.10.0"
val sbtJmh             = "0.4.8"
val webjarsLocatorCore = "0.59"
val sbtHeader          = "5.11.0"
val scalafmt           = "2.4.6"
val sbtTwirl: String   = sys.props.getOrElse("twirl.version", "1.6.10") // sync with documentation/project/plugins.sbt

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackager,
  "sbtTwirlVersion"          -> sbtTwirl,
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play"  % "sbt-twirl"             % sbtTwirl)
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin"       % mima)
addSbtPlugin("com.lightbend.sbt"  % "sbt-bill-of-materials" % "1.0.2")
addSbtPlugin("com.github.sbt"     % "sbt-java-formatter"    % sbtJavaFormatter)
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % sbtJmh)
addSbtPlugin("com.github.sbt"     % "sbt-header"            % sbtHeader)
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"          % scalafmt)
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"        % "1.11.2")

addSbtPlugin("com.markatta" % "sbt-akka-version-check" % "0.4")

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % webjarsLocatorCore
)
