//
// Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
//
import scala.reflect._

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := sys.props.get("scala.version").getOrElse("2.10.4")

sources in (Compile, routes) := Seq(baseDirectory.value / "routes")

InputKey[Unit]("all-problems-are-from") := {
  val args = Def.spaceDelimited("<source> <line>").parsed
  val base: File = baseDirectory.value
  val source = base / args(0)
  val line = Integer.parseInt(args(1))
  Incomplete.allExceptions(assertLeft(assertSome(Project.runTask(compile in Compile, state.value))._2.toEither)).flatMap {
    case cf: xsbti.CompileFailed => cf.problems()
    case other => throw other
  }.map { problem =>
    val problemSource = assertSome(problem.position().sourceFile())
    val problemLine = assertSome(problem.position().line())
    if (problemSource.getCanonicalPath != source.getCanonicalPath)
      throw new Exception("Problem from wrong source file: " + problemSource)
    if (problemLine != line)
      throw new Exception("Problem from wrong source file line: " + line)
    println(s"Problem: ${problem.message()} at $problemSource:$problemLine validated")
    ()
  }.headOption.getOrElse(throw new Exception("No errors were validated"))
}

def assertSome[T: ClassTag](o: Option[T]): T = {
  o.getOrElse(throw new Exception("Expected Some[" + implicitly[ClassTag[T]] + "]"))
}

def assertSome[T: ClassTag](m: xsbti.Maybe[T]): T = {
  if (m.isEmpty) throw new Exception("Expected Some[" + implicitly[ClassTag[T]] + "]")
  else m.get()
}

def assertLeft[T: ClassTag](e: Either[T, _]) = {
  e.left.getOrElse(throw new Exception("Expected Left[" + implicitly[ClassTag[T]] + "]"))
}
