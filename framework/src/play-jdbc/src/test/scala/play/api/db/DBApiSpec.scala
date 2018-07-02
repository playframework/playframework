/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import org.specs2.mutable.Specification
import play.api.test.WithApplication

class DBApiSpec extends Specification {

  "DBApi" should {

    "start the application even when database is not available" in new WithApplication(_.configure(
      // a bogus URL which is not accepted by H2, just to prove that
      // the application will start without a proper database config.
      "db.default.url" -> "jdbc:bogus://localhost:1234",
      "db.default.driver" -> "org.h2.Driver"
    )) {
      val dependsOnDbApi = app.injector.instanceOf[DependsOnDbApi]
      dependsOnDbApi.dBApi must not beNull
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

case class DependsOnDbApi(dBApi: DBApi)