// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(BuildInfoPlugin)

// when updating sbtNativePackager version, be sure to also update the documentation links in
// documentation/manual/working/commonGuide/production/Deploying.md
val sbtNativePackager  = "1.9.16"
val mima               = "1.1.3"
val sbtJavaFormatter   = "0.8.0"
val sbtJmh             = "0.4.6"
val webjarsLocatorCore = "0.53"
val sbtHeader          = "5.8.0"
val scalafmt           = "2.4.6"
val sbtTwirl: String   = sys.props.getOrElse("twirl.version", "1.6.0-RC4") // sync with documentation/project/plugins.sbt
val interplay: String  = sys.props.getOrElse("interplay.version", "3.1.5")

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackager,
  "sbtTwirlVersion"          -> sbtTwirl,
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play"  % "interplay"             % interplay)
addSbtPlugin("com.typesafe.play"  % "sbt-twirl"             % sbtTwirl)
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin"       % mima)
addSbtPlugin("com.lightbend.sbt"  % "sbt-bill-of-materials" % "1.0.2")
addSbtPlugin("com.lightbend.sbt"  % "sbt-java-formatter"    % sbtJavaFormatter)
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % sbtJmh)
addSbtPlugin("de.heikoseeberger"  % "sbt-header"            % sbtHeader)
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"          % scalafmt)
addSbtPlugin("com.github.sbt"     % "sbt-ci-release"        % "1.5.12")

addSbtPlugin("com.lightbend.akka" % "sbt-akka-version-check" % "0.1")

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % webjarsLocatorCore
)

resolvers += Resolver.typesafeRepo("releases")
