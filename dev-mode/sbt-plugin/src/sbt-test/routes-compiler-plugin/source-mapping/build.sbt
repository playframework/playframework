//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//
import Common._
import scala.reflect._

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(MediatorWorkaroundPlugin)

libraryDependencies += guice

scalaVersion := sys.props.get("scala.version").getOrElse("2.12.9")

sources in (Compile, routes) := Seq(baseDirectory.value / "routes")

InputKey[Unit]("allProblemsAreFrom") := {
  val args       = Def.spaceDelimited("<source> <line>").parsed
  val base: File = baseDirectory.value
  val source     = base / args(0)
  val line       = Integer.parseInt(args(1))
  Incomplete
    .allExceptions(assertLeft(assertSome(Project.runTask(compile in Compile, state.value))._2.toEither))
    .flatMap {
      case cf: xsbti.CompileFailed => cf.problems()
      case other                   => throw other
    }
    .map { problem =>
      val problemSource = assertNotEmpty(problem.position().sourceFile())
      val problemLine   = assertNotEmpty(problem.position().line())
      if (problemSource.getCanonicalPath != source.getCanonicalPath)
        throw new Exception("Problem from wrong source file: " + problemSource)
      if (problemLine != line)
        throw new Exception("Problem from wrong source file line: " + line)
      println(s"Problem: ${problem.message()} at $problemSource:$problemLine validated")
      ()
    }
    .headOption
    .getOrElse(throw new Exception("No errors were validated"))
}

def assertSome[T: ClassTag](o: Option[T]): T = {
  o.getOrElse(throw new Exception("Expected Some[" + implicitly[ClassTag[T]] + "]"))
}

def assertLeft[T: ClassTag](e: Either[T, _]) = {
  e.left.getOrElse(throw new Exception("Expected Left[" + implicitly[ClassTag[T]] + "]"))
}

// This is copy/pasted from AkkaSnapshotRepositories since scripted tests also need
// the snapshot resolvers in `cron` builds.
// If this is a cron job in Travis:
// https://docs.travis-ci.com/user/cron-jobs/#detecting-builds-triggered-by-cron
resolvers in ThisBuild ++= (sys.env.get("TRAVIS_EVENT_TYPE").filter(_.equalsIgnoreCase("cron")) match {
  case Some(_) =>
    Seq(
      "akka-snapshot-repository".at("https://repo.akka.io/snapshots"),
      "akka-http-snapshot-repository".at("https://dl.bintray.com/akka/snapshots/")
    )
  case None => Seq.empty
})
