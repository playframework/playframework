/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import org.specs2.mutable.Specification
import play.api.inject._
import play.api.test.WithApplication

class DBApiSpec extends Specification {

  "DBApi" should {

    "not avoid application start when there is a configuration error" in new WithApplication(_.configure(
      // a bogus URL which is not accepted by H2, just to prove that
      // the application will start without a proper database config.
      "db.default.url" -> "jdbc:bogus://localhost:1234",
      "db.default.driver" -> "org.h2.Driver"
    ).bindings(
        // We then add some component that we will try to access
        // to check that other components are successfully instanced
        // when there is a database misconfiguration.
        bind[EagerComponent].toInstance(EagerComponent(true)))
    ) {
      val eagerComponent = app.injector.instanceOf[EagerComponent]
      eagerComponent.started must beTrue
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

case class EagerComponent(started: Boolean)