//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

lazy val root = (project in file("."))
  .enablePlugins(PlayService)
  .settings(
    scalaVersion := (sys.props("scala.crossversions").split(" ").toSeq.filter(v => SemanticSelector(sys.props("scala.version")).matches(VersionNumber(v))) match {
      case Nil => sys.error("Unable to detect scalaVersion! Did you pass scala.crossversions and scala.version Java properties?")
      case Seq(version) => version
      case multiple => sys.error(s"Multiple crossScalaVersions matched query '${sys.props("scala.version")}': ${multiple.mkString(", ")}")
    }),
    updateOptions := updateOptions.value.withLatestSnapshots(false),
    update / evictionWarningOptions ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false)),
    PlayKeys.playInteractionMode := play.sbt.StaticPlayNonBlockingInteractionMode,
    libraryDependencies += guice,
    InputKey[Unit]("verifyResourceContains") := {
      val args       = Def.spaceDelimited("<path> <status> <words> ...").parsed
      val path       = args.head
      val status     = args.tail.head.toInt
      val assertions = args.tail.tail
      ScriptedTools.verifyResourceContains(path, status, assertions)
    }
  )
