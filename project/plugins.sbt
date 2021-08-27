// Copyright (C) Lightbend Inc. <https://www.lightbend.com>

enablePlugins(BuildInfoPlugin)

// when updating sbtNativePackager version, be sure to also update the documentation links in
// documentation/manual/working/commonGuide/production/Deploying.md
val sbtNativePackager  = "1.8.1"
val mima               = "0.9.2"
val sbtJavaFormatter   = "0.5.0"
val sbtJmh             = "0.4.3"
val webjarsLocatorCore = "0.47"
val sbtHeader          = "5.6.0"
val scalafmt           = "2.0.1"
val sbtTwirl: String   = sys.props.getOrElse("twirl.version", "1.5.1") // sync with documentation/project/plugins.sbt
val interplay: String  = sys.props.getOrElse("interplay.version", "3.0.3")

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackager,
  "sbtTwirlVersion"          -> sbtTwirl,
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("com.typesafe.play"  % "interplay"             % interplay)
addSbtPlugin("com.typesafe.sbt"   % "sbt-twirl"             % sbtTwirl)
addSbtPlugin("com.typesafe"       % "sbt-mima-plugin"       % mima)
addSbtPlugin("com.lightbend.sbt"  % "sbt-bill-of-materials" % "1.0.2")
addSbtPlugin("com.lightbend.sbt"  % "sbt-java-formatter"    % sbtJavaFormatter)
addSbtPlugin("pl.project13.scala" % "sbt-jmh"               % sbtJmh)
addSbtPlugin("de.heikoseeberger"  % "sbt-header"            % sbtHeader)
addSbtPlugin("org.scalameta"      % "sbt-scalafmt"          % scalafmt)
addSbtPlugin("com.dwijnand"       % "sbt-dynver"            % "4.1.1")

addSbtPlugin("com.lightbend.akka" % "sbt-akka-version-check" % "0.1")

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % webjarsLocatorCore
)

resolvers += Resolver.typesafeRepo("releases")
