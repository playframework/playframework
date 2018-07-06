/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

import javax.inject.Inject
import org.specs2.mutable.Specification
import play.api.PlayException
import play.api.test.WithApplication

class DBApiSpec extends Specification {

  "DBApi" should {

    "start the application even when database is not available" in new WithApplication(_.configure(
      // Here we have a URL that is valid for H2, but the database is not available.
      // We should not fail to start the application here.
      "db.default.url" -> "jdbc:h2:tcp://localhost/~/bogus",
      "db.default.driver" -> "org.h2.Driver"
    )) {
      val dependsOnDbApi = app.injector.instanceOf[DependsOnDbApi]
      dependsOnDbApi.dBApi must not beNull
    }

    "fail to start the application even when there is a database misconfiguration" in {
      new WithApplication(_.configure(
        // Having a wrong configuration like an invalid url is different from having
        // a valid configuration where the database is not available yet. We should
        // fail fast and report this since it is a programming error.
        "db.default.url" -> "jdbc:bogus://localhost",
        "db.default.driver" -> "org.h2.Driver"
      )) {} must throwA[PlayException]
    }

    "fail to start the application even when database is not available and configured to fail fast" in {
      new WithApplication(_.configure(
        // Here we have a URL that is valid for H2, but the database is not available.
        "db.default.url" -> "jdbc:bogus://localhost",
        "db.default.driver" -> "org.h2.Driver",
        // This overrides the default configuration and makes HikariCP fails fast.
        "play.db.prototype.hikaricp.initializationFailTimeout" -> "1"
      )) {} must throwA[PlayException]
    }

    "create all the configured databases" in new WithApplication(_.configure(
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
      dbApi.database("default").url must beEqualTo("jdbc:h2:mem:default")
      dbApi.database("test").url must beEqualTo("jdbc:h2:mem:test")
      dbApi.database("other").url must beEqualTo("jdbc:h2:mem:other")
    }
  }
}

case class DependsOnDbApi @Inject() (dBApi: DBApi) {
  // eagerly access the database but without trying to connect to it.
  dBApi.database("default").dataSource
}