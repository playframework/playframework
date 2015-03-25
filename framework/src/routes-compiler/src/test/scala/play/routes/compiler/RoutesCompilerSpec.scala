/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.routes.compiler

import java.io.File

import org.specs2.mutable.Specification
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

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

    "generate routes classes for route definitions that pass the checks" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("generating.routes").toURI)
      RoutesCompiler.compile(RoutesCompilerTask(file, Seq.empty, true, true, false), StaticRoutesGenerator, tmp)

      val generatedRoutes = new File(tmp, "generating/routes_routing.scala")
      generatedRoutes.exists() must beTrue

      val generatedReverseRoutes = new File(tmp, "generating/routes_reverseRouting.scala")
      generatedReverseRoutes.exists() must beTrue
    }

    "check if there are no routes using overloaded handler methods" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("duplicateHandlers.routes").toURI)
      RoutesCompiler.compile(RoutesCompilerTask(file, Seq.empty, true, true, false), StaticRoutesGenerator, tmp) must beLeft
    }

    "check if routes with type projection are compiled" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("complexTypes.routes").toURI)
      RoutesCompiler.compile(RoutesCompilerTask(file, Seq.empty, true, true, false), StaticRoutesGenerator, tmp) must beRight
    }
  }
}
