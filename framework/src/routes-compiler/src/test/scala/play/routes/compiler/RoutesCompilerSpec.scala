/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routes.compiler

import java.io.File

import org.specs2.mutable.Specification

import scala.io.Source

object RoutesCompilerSpec extends Specification {

  sequential

  "route file compiler" should {

    def withTempDir[T](block: File => T) = {
      val tmp = File.createTempFile("RoutesCompilerSpec", "")
      tmp.delete()
      tmp.mkdir()
      try {
        block(tmp)
      } finally {
        def rm(file: File): Unit = file match {
          case dir if dir.isDirectory =>
            dir.listFiles().foreach(rm)
            dir.delete()
          case f => f.delete()
        }
        rm(tmp)
      }
    }

    "not generate reverse ref routing if its disabled" in withTempDir { tmp =>
      val f = new File(this.getClass.getClassLoader.getResource("generating.routes").toURI)
      RoutesCompiler.compile(f, StaticRoutesGenerator, tmp, Seq.empty, generateReverseRouter = true,
        generateRefReverseRouter = false)

      val generatedJavaRoutes = new File(tmp, "controllers/routes.java")
      val contents = scala.io.Source.fromFile(generatedJavaRoutes).getLines().mkString("")
      contents.contains("public static class ref") must beFalse
    }

    "generate routes classes for route definitions that pass the checks" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("generating.routes").toURI)
      RoutesCompiler.compile(file, StaticRoutesGenerator, tmp, Seq())

      val generatedRoutes = new File(tmp, "generating/routes_routing.scala")
      generatedRoutes.exists() must beTrue

      val generatedReverseRoutes = new File(tmp, "generating/routes_reverseRouting.scala")
      generatedReverseRoutes.exists() must beTrue
    }

    "check if there are no routes using overloaded handler methods" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("duplicateHandlers.routes").toURI)
      RoutesCompiler.compile(file, StaticRoutesGenerator, tmp, Seq.empty) must beLeft
    }

    "check if routes with type projection are compiled" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("complexTypes.routes").toURI)
      object A {
        type B = Int
      }
      RoutesCompiler.compile(file, StaticRoutesGenerator, tmp, Seq.empty) must beRight
    }
  }
}
