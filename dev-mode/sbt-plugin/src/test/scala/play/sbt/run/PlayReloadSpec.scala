/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt.run

import java.io.File
import java.nio.file.Paths
import java.util.Optional

import sbt.internal.inc.MappedFileConverter

import org.specs2.mutable.Specification
import xsbti.FileConverter
import xsbti.Position

class PlayReloadSpec extends Specification {
  "PlayReload.mapPosition" should {
    "resolve virtual source paths through the file converter" in {
      val base                                  = Paths.get("target", "base").toAbsolutePath
      val out                                   = Paths.get("target", "out").toAbsolutePath
      implicit val fileConverter: FileConverter = MappedFileConverter(Map("BASE" -> base, "OUT" -> out), true)

      val mapped = PlayReload.mapPosition(position("${OUT}/routes/main/router/Routes.scala"), Nil)

      mapped.sourceFile().get().toPath must_== out.resolve("routes/main/router/Routes.scala")
      mapped.sourcePath().get() must_== out.resolve("routes/main/router/Routes.scala").toString
    }

    "retain the working-directory fallback for an unknown virtual root" in {
      val base                                  = Paths.get("target", "base").toAbsolutePath
      implicit val fileConverter: FileConverter = MappedFileConverter(Map("BASE" -> base), true)

      val mapped   = PlayReload.mapPosition(position("${UNKNOWN}/app/controllers/HomeController.scala"), Nil)
      val expected = Paths
        .get("app", "controllers", "HomeController.scala")
        .toAbsolutePath

      mapped.sourceFile().get().toPath must_== expected
      mapped.sourcePath().get() must_== expected.toString
    }
  }

  private def position(path: String): Position = new Position {
    override def line(): Optional[Integer]        = Optional.empty()
    override def lineContent(): String            = ""
    override def offset(): Optional[Integer]      = Optional.empty()
    override def pointer(): Optional[Integer]     = Optional.empty()
    override def pointerSpace(): Optional[String] = Optional.empty()
    override def sourcePath(): Optional[String]   = Optional.of(path)
    override def sourceFile(): Optional[File]     = Optional.empty()
  }
}
