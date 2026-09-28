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

    def copyResource(name: String, directory: File): File = {
      val file   = new File(directory, name)
      val stream = Option(this.getClass.getClassLoader.getResourceAsStream(name)).getOrElse {
        throw new IllegalArgumentException(s"Resource not found: $name")
      }
      try Files.copy(stream, file.toPath)
      finally stream.close()
      file
    }

    "generate routes classes for route definitions that pass the checks" in withTempDir { tmp =>
      val file = copyResource("generating.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false, Language.SCALA),
        InjectedRoutesGenerator,
        tmp
      )

      new File(tmp, "generating/Routes.scala") must exist
      new File(tmp, "generating/RoutesPrefix.scala") must exist
      new File(tmp, "controllers/ReverseRoutes.scala") must exist
      new File(tmp, "controllers/javascript/JavaScriptReverseRoutes.scala") must exist
      new File(tmp, "controllers/routes.java") must exist
    }

    "generate Java routes classes when requested" in withTempDir { tmp =>
      val file = copyResource("generating.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false, Language.JAVA),
        InjectedRoutesGenerator,
        tmp
      ) must beRight

      new File(tmp, "generating/Routes.java") must exist
      new File(tmp, "generating/RoutesPrefix.java") must exist
      new File(tmp, "controllers/ReverseRoutes.java") must exist
      new File(tmp, "controllers/javascript/JavaScriptReverseRoutes.java") must exist
      new File(tmp, "controllers/routes.java") must exist
      new File(tmp, "generating/Routes.scala") must not be exist
      new File(tmp, "controllers/ReverseRoutes.scala") must not be exist
    }

    "keep Scala routes as the default for the compatibility APIs" in withTempDir { tmp =>
      val file = copyResource("generating.routes", tmp)
      val task = RoutesCompilerTask(file, Seq.empty, true, true, true, false)

      task.lang must beEqualTo(Language.SCALA)
      task.copy(file, Seq.empty, true, true, true, false).lang must beEqualTo(Language.SCALA)
      (task match {
        case RoutesCompilerTask(extractedFile, _, _, _, _, _) => Some(extractedFile)
        case _                                                => None
      }) must beSome(file)
      RoutesCompilerTask.unapplyWithLanguage(task).map(_._7) must beSome(Language.SCALA)
      RoutesCompiler.compile(file, java.util.List.of(), true, true, true, false, tmp) must beRight

      new File(tmp, "generating/Routes.scala") must exist
      new File(tmp, "generating/Routes.java") must not be exist
    }

    "do not generate JavaScript routes when disabled in task" in withTempDir { tmp =>
      val file = copyResource("generating.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, false, false, Language.SCALA),
        InjectedRoutesGenerator,
        tmp
      )
      new File(tmp, "controllers/javascript/JavaScriptReverseRoutes.scala") must not be exist
    }

    "check if there are no routes using overloaded handler methods" in withTempDir { tmp =>
      val file = copyResource("duplicateHandlers.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false, Language.SCALA),
        InjectedRoutesGenerator,
        tmp
      ) must beLeft
    }

    "check if routes with type projection are compiled" in withTempDir { tmp =>
      val file = copyResource("complexTypes.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false, Language.SCALA),
        InjectedRoutesGenerator,
        tmp
      ) must beRight
    }

    "check if routes with complex names are compiled" in withTempDir { tmp =>
      val file = copyResource("complexNames.routes", tmp)
      RoutesCompiler.compile(
        RoutesCompilerTask(file, Seq.empty, true, true, true, false, Language.SCALA),
        InjectedRoutesGenerator,
        tmp
      ) must beRight
    }
  }
}
