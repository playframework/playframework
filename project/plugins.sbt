// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

enablePlugins(BuildInfoPlugin)

addDependencyTreePlugin

// when updating sbtNativePackager version, be sure to also update the documentation links in
// documentation/manual/working/commonGuide/production/Deploying.md
val sbtNativePackager  = "1.10.4"
val mima               = "1.1.4"
val sbtJavaFormatter   = "0.8.0"
val sbtJmh             = "0.4.7"
val webjarsLocatorCore = "0.59"
val sbtHeader          = "5.8.0"
val scalafmt           = "2.4.6"
val sbtTwirl: String   = sys.props.getOrElse("twirl.version", "2.1.0-M1") // sync with documentation/project/plugins.sbt

buildInfoKeys := Seq[BuildInfoKey](
  "sbtNativePackagerVersion" -> sbtNativePackager,
  "sbtTwirlVersion"          -> sbtTwirl,
)

logLevel := Level.Warn

scalacOptions ++= Seq("-deprecation", "-language:_")

addSbtPlugin("org.playframework.twirl" % "sbt-twirl"             % sbtTwirl)
addSbtPlugin("com.typesafe"            % "sbt-mima-plugin"       % mima)
addSbtPlugin("com.lightbend.sbt"       % "sbt-bill-of-materials" % "1.0.2")
addSbtPlugin("com.lightbend.sbt"       % "sbt-java-formatter"    % sbtJavaFormatter)
addSbtPlugin("pl.project13.scala"      % "sbt-jmh"               % sbtJmh)
addSbtPlugin("de.heikoseeberger"       % "sbt-header"            % sbtHeader)
addSbtPlugin("org.scalameta"           % "sbt-scalafmt"          % scalafmt)
addSbtPlugin("com.github.sbt"          % "sbt-ci-release"        % "1.6.1")
// sbt-ci-release relies on sbt-git, which in turn depends on jgit. Unfortunately, sbt-git is still using jgit v5
// to maintain support for Java 8, while jgit v6 requires Java 11, and jgit v7 requires Java 17. Since jgit v7
// finally introduces (read-only) support for git worktree and Play already requires Java 17, we can upgrade jgit
// ourselves to enhance the developer experience for Play contributors, especially if they want to use git worktree.
// See https://github.com/sbt/sbt-git/issues/213 and https://github.com/sbt/sbt-git/pull/243#issuecomment-2397762074
libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "7.0.0.202409031743-r"

addSbtPlugin("nl.gn0s1s" % "sbt-pekko-version-check" % "0.0.5")

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % webjarsLocatorCore
)
