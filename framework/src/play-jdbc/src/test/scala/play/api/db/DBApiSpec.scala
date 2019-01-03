/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import javax.inject.Inject
import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{ Application, Environment, Mode, PlayException }
import play.api.test.WithApplication

class TestDBApiSpec extends DBApiSpec(Mode.Test)
class DevDBApiSpec extends DBApiSpec(Mode.Dev)
class ProdDBApiSpec extends DBApiSpec(Mode.Prod)

abstract class DBApiSpec(mode: Mode) extends Specification {

  def app(conf: (String, Any)*): Application = {
    GuiceApplicationBuilder(environment = Environment.simple(mode = mode))
      .configure(conf: _*)
      .build()
  }

  "DBApi" should {

    "start the application when database is not available" in new WithApplication(app(
      // Here we have a URL that is valid for H2, but the database is not available.
      // We should not fail to start the application here.
      "db.default.url" -> "jdbc:h2:tcp://localhost/~/notavailable",
      "db.default.driver" -> "org.h2.Driver"
    )) {
      val dependsOnDbApi = app.injector.instanceOf[DependsOnDbApi]
      dependsOnDbApi.dBApi must not beNull
    }

    "fail to start the application when there is a database misconfiguration" in {
      new WithApplication(app(
        // Having a wrong configuration like an invalid url is different from having
        // a valid configuration where the database is not available yet. We should
        // fail fast and report this since it is a programming error.
        "db.default.url" -> "jdbc:bogus://localhost",
        "db.default.driver" -> "org.h2.Driver"
      )) {} must throwA[PlayException]
    }

    "fail to start the application when database is not available and configured to fail fast" in {
      new WithApplication(app(
        // Here we have a URL that is valid for H2, but the database is not available.
        "db.default.url" -> "jdbc:bogus://localhost",
        "db.default.driver" -> "org.h2.Driver",
        // This overrides the default configuration and makes HikariCP fails fast.
        "play.db.prototype.hikaricp.initializationFailTimeout" -> "1"
      )) {} must throwA[PlayException]
    }

    "correct report the configuration error" in {
      new WithApplication(app(
        // The configuration is correct, but the database is not available
        "db.default.url" -> "jdbc:h2:tcp://localhost/~/notavailable",
        "db.default.driver" -> "org.h2.Driver",

        // The configuration is correct and the database is available
        "db.test.url" -> "jdbc:h2:mem:test",
        "db.test.driver" -> "org.h2.Driver",

        // The configuration is incorrect, so we should report an error
        "db.bogus.url" -> "jdbc:bogus://localhost",
        "db.bogus.driver" -> "org.h2.Driver"
      )) {} must throwA[PlayException]("Configuration error\\[Cannot initialize to database \\[bogus\\]\\]")
    }

    "correct report the configuration error when there is not URL configured" in {
      new WithApplication(app(
        // Missing url configuration
        "db.test.driver" -> "org.h2.Driver"
      )) {} must throwA[PlayException]("Configuration error\\[Cannot initialize to database \\[test\\]\\]")
    }

    "create all the configured databases" in new WithApplication(app(
      // default
      "db.default.url" -> "jdbc:h2:mem:default",
      "db.default.driver" -> "org.h2.Driver",

      // test
      "db.test.url" -> "jdbc:h2:mem:test",
      "db.test.driver" -> "org.h2.Driver",

      // other
      "db.other.url" -> "jdbc:h2:mem:other",
      "db.other.driver" -> "org.h2.Driver"
    )) {
      val dbApi = app.injector.instanceOf[DBApi]
      dbApi.database("default").url must startingWith("jdbc:h2:mem:default")
      dbApi.database("test").url must startingWith("jdbc:h2:mem:test")
      dbApi.database("other").url must startingWith("jdbc:h2:mem:other")
    }
  }
}

case class DependsOnDbApi @Inject() (dBApi: DBApi) {
  // eagerly access the database but without trying to connect to it.
  dBApi.database("default").dataSource
}