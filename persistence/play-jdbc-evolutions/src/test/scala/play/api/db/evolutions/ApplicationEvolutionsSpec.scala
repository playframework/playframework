/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db.evolutions

import java.io.File
import java.util.UUID

import org.mockito.Mockito
import org.specs2.mutable.After
import org.specs2.mutable.Specification
import play.api.db.DBApi
import play.api.db.Database
import play.api.db.Databases
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.Configuration
import play.api.Environment
import play.api.Mode
import play.core.BuildLink
import play.core.DefaultWebCommands

class ApplicationEvolutionsSpec extends Specification {
  sequential

  "ApplicationEvolutions" should {

    "start in DEV mode although the database is in an inconsistent state" in new WithInconsistentDatabase {
      start(Mode.Dev).upToDate must beFalse
    }

    "let the evolutions web commands handle the inconsistent state in DEV mode" in new WithInconsistentDatabase {
      start(Mode.Dev)

      // Other requests stay blocked
      handle("/") must throwAn[InconsistentDatabase]

      val result = handle("/@evolutions/resolve/default/1", "redirect" -> "/foo")
      result.map(_.header.status) must beSome(303)
      result.flatMap(_.header.headers.get("Location")) must beSome("/foo")
      forcedReloads must_== 1

      // The reloaded application is up to date
      start(Mode.Dev).upToDate must beTrue
    }

    "fail to start in PROD mode if the database is in an inconsistent state" in new WithInconsistentDatabase {
      start(Mode.Prod) must throwAn[InconsistentDatabase]
    }

    "fail to start in TEST mode if the database is in an inconsistent state" in new WithInconsistentDatabase {
      start(Mode.Test) must throwAn[InconsistentDatabase]
    }

    "not nest the URL to return to when applying or resolving evolutions" in {
      InvalidDatabaseRevision("default", "script").htmlDescription must contain("get('redirect')")
      InconsistentDatabase("default", "script", "error", 1, autocommit = true).htmlDescription must contain(
        "get('redirect')"
      )
    }
  }

  trait WithInconsistentDatabase extends After {
    lazy val db: Database =
      Databases("org.h2.Driver", s"jdbc:h2:mem:application-evolutions-${UUID.randomUUID()}", "default")

    lazy val dbApi: DBApi = new DBApi {
      def databases(): Seq[Database]       = Seq(db)
      def database(name: String): Database = db
      def shutdown(): Unit                 = db.shutdown()
    }

    lazy val evolutionsApi = new DefaultEvolutionsApi(dbApi)
    lazy val config        = new DefaultEvolutionsConfigParser(Configuration.reference).get
    lazy val reader        =
      SimpleEvolutionsReader.forDefault(Evolution(1, "creaTYPOe table test (id bigint);", "drop table test;"))

    // Applying the broken evolution leaves the database in an inconsistent state
    try evolutionsApi.evolve("default", evolutionsApi.scripts("default", reader, ""), autocommit = true, "")
    catch { case _: InconsistentDatabase => }

    var forcedReloads = 0
    val buildLink     = new BuildLink {
      def reload(): AnyRef                                            = null
      def findSource(className: String, line: Integer): Array[AnyRef] = null
      def projectPath(): File                                         = new File(".")
      def forceReload(): Unit                                         = forcedReloads += 1
      def settings(): java.util.Map[String, String]                   = java.util.Collections.emptyMap()
    }

    val webCommands = new DefaultWebCommands

    def start(mode: Mode): ApplicationEvolutions =
      new ApplicationEvolutions(
        config,
        reader,
        evolutionsApi,
        new DynamicEvolutions,
        dbApi,
        Environment.simple(mode = mode),
        webCommands
      )

    def handle(path: String, query: (String, String)*): Option[Result] = {
      val request = Mockito.mock(classOf[RequestHeader])
      Mockito.when(request.path).thenReturn(path)
      Mockito.when(request.queryString).thenReturn(query.map { case (k, v) => k -> Seq(v) }.toMap)
      webCommands.handleWebCommand(request, buildLink, new File("."))
    }

    def after: Any = db.shutdown()
  }
}
