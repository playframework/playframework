/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.nio.file.attribute.FileTime
import java.nio.file.Files
import java.nio.file.Path

import scala.jdk.CollectionConverters._
import scala.util.Using

import org.specs2.mutable.Specification
import play.api.Environment
import play.api.Mode

class UpdateEvolutionScriptSpec extends Specification {

  /**
   * Runs the block with a temporary folder, which contains the application's root folder `app`, and leaves room for
   * folders next to it.
   */
  private def withFolder[T](block: (Path, Environment) => T): T = {
    val folder = Files.createTempDirectory("update-evolution-script")
    try {
      val root = Files.createDirectories(folder.resolve("app"))
      block(folder, Environment.simple(root.toFile, Mode.Dev))
    } finally {
      Using.resource(Files.walk(folder))(_.iterator().asScala.toList).reverse.foreach(Files.delete)
    }
  }

  /** The evolution scripts in the folder, relative to it. */
  private def scripts(folder: Path): Seq[Path] =
    Using.resource(Files.walk(folder))(
      _.iterator().asScala.filter(_.getFileName.toString == "1.sql").map(folder.relativize).toList
    )

  /** The evolutions that Play's evolutions read from the path. */
  private def read(path: String)(implicit environment: Environment): Seq[(Int, String, String)] =
    new EnvironmentEvolutionsReader(environment, path)
      .evolutions("default")
      .map(evolution => (evolution.revision, evolution.sql_up.trim, evolution.sql_down.trim))

  private def update(path: String, ups: String)(implicit environment: Environment): Unit =
    Evolutions.updateEvolutionScript(path = path, ups = ups, downs = "drop table a;")

  private def writes(path: String, script: String) = withFolder { (folder, env) =>
    implicit val environment: Environment = env
    update(path, "create table a (id int);")
    (read(path) must_== Seq((1, "create table a (id int);", "drop table a;")))
      .and(scripts(folder) must_== Seq(Path.of(script)))
  }

  "Evolutions.updateEvolutionScript" should {

    "create the first script, and its folders" in writes("evolutions", "app/conf/evolutions/default/1.sql")

    "write to a folder in conf" in writes("migrations", "app/conf/migrations/default/1.sql")

    "write to a folder in the project" in writes("./migrations", "app/migrations/default/1.sql")

    "write to a folder next to the project" in writes("../migrations", "migrations/default/1.sql")

    "write to an absolute folder as it is" in withFolder { (folder, env) =>
      implicit val environment: Environment = env
      val path                              = folder.resolve("migrations").toAbsolutePath.toString
      update(path, "create table a (id int);")
      (read(path) must_== Seq((1, "create table a (id int);", "drop table a;")))
        .and(scripts(folder) must_== Seq(Path.of("migrations/default/1.sql")))
    }

    "update an existing script" in withFolder { (folder, env) =>
      implicit val environment: Environment = env
      update("evolutions", "create table a (id int);")
      update("evolutions", "create table a (id bigint);")
      read("evolutions") must_== Seq((1, "create table a (id bigint);", "drop table a;"))
    }

    "keep an unchanged script as it is" in withFolder { (folder, env) =>
      implicit val environment: Environment = env
      update("evolutions", "create table a (id int);")
      val script   = folder.resolve("app/conf/evolutions/default/1.sql")
      val modified = FileTime.fromMillis(1000)
      Files.setLastModifiedTime(script, modified)
      update("evolutions", "create table a (id int);")
      Files.getLastModifiedTime(script) must_== modified
    }
  }
}
