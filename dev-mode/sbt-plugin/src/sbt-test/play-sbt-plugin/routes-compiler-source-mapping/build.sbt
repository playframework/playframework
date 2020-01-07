//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

import scala.reflect.{ ClassTag, classTag }

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

libraryDependencies += guice

scalaVersion := sys.props("scala.version")
updateOptions := updateOptions.value.withLatestSnapshots(false)
evictionWarningOptions in update ~= (_.withWarnTransitiveEvictions(false).withWarnDirectEvictions(false))

sources in (Compile, routes) := Seq(baseDirectory.value / "routes")

InputKey[Unit]("allProblemsAreFrom") := {
  val args   = Def.spaceDelimited("<source> <line>").parsed
  val base   = baseDirectory.value
  val source = base / args(0)
  val line   = Integer.parseInt(args(1))
  val errors = Incomplete
    .allExceptions(assertLeft(assertSome(Project.runTask(compile in Compile, state.value))._2.toEither))
    .flatMap {
      case cf: xsbti.CompileFailed => cf.problems()
      case other                   => throw other
    }
  if (errors.isEmpty) sys.error("No errors were validated")
  errors.foreach { problem =>
    val problemSource = assertNotEmpty(problem.position().sourceFile())
    val problemLine   = assertNotEmpty(problem.position().line())
    if (problemSource.getCanonicalPath != source.getCanonicalPath)
      sys.error(s"Problem from wrong source file: $problemSource")
    if (problemLine != line)
      sys.error(s"Problem from wrong source file line: $line")
    println(s"Problem: ${problem.message()} at $problemSource:$problemLine validated")
  }
}

def assertSome[T: ClassTag](o: Option[T]): T = {
  o.getOrElse(sys.error(s"Expected Some[${classTag[T]}]"))
}

def assertLeft[T: ClassTag](e: Either[T, _]) = {
  e.left.getOrElse(sys.error(s"Expected Left[${classTag[T]}]"))
}

def assertNotEmpty[T: ClassTag](o: java.util.Optional[T]): T = {
  if (o.isPresent) o.get()
  else throw new Exception(s"Expected Some[${classTag[T]}]")
}
