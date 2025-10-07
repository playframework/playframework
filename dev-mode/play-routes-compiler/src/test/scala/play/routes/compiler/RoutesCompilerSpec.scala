/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routes.compiler

import java.io.File
import java.nio.file.Files

import org.specs2.matcher.FileMatchers
import org.specs2.mutable.Specification
import play.routes.compiler.RoutesCompiler.RoutesCompilerTask

class RoutesCompilerSpec extends Specification with FileMatchers {
  sequential

  "route file compiler" should {
    def withTempDir[T](block: File => T) = {
      val tmp = Files.createTempFile("RoutesCompilerSpec", "").toFile
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
      RoutesCompiler.compile(RoutesCompilerTask(file, Seq.empty, true, true, true, false), InjectedRoutesGenerator, tmp)

      new File(tmp, "generating/Routes.scala") must exist
      new File(tmp, "generating/RoutesPrefix.scala") must exist
      new File(tmp, "controllers/ReverseRoutes.scala") must exist
      new File(tmp, "controllers/javascript/JavaScriptReverseRoutes.scala") must exist
      new File(tmp, "controllers/routes.java") must exist
    }

    "do not generate JavaScript routes when disabled in task" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("generating.routes").toURI)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, false, false),
        InjectedRoutesGenerator,
        tmp
      )
      new File(tmp, "controllers/javascript/JavaScriptReverseRoutes.scala") must not be exist
    }

    "check if there are no routes using overloaded handler methods" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("duplicateHandlers.routes").toURI)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false),
        InjectedRoutesGenerator,
        tmp
      ) must beLeft
    }

    "check if routes with type projection are compiled" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("complexTypes.routes").toURI)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false),
        InjectedRoutesGenerator,
        tmp
      ) must beRight
    }

    "check if routes with complex names are compiled" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("complexNames.routes").toURI)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false),
        InjectedRoutesGenerator,
        tmp
      ) must beRight
    }

    "check if routes with reserved words as method name are compiled" in withTempDir { tmp =>
      val file = new File(this.getClass.getClassLoader.getResource("reservedWords.routes").toURI)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, false),
        InjectedRoutesGenerator,
        tmp
      ) must beRight
    }
  }
}
