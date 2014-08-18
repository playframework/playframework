/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.Inject
import play.api.test._

class NamedDatabaseSpec extends PlaySpecification {

  "DBModule" should {

    "bind databases by name" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "db.default.driver" -> "org.h2.Driver",
        "db.default.url" -> "jdbc:h2:mem:default",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      val dbComponent = app.injector.instanceOf[DbComponent]
      dbComponent.default.url must_== "jdbc:h2:mem:default"
      dbComponent.named.url must_== "jdbc:h2:mem:default"
      dbComponent.other.url must_== "jdbc:h2:mem:other"
    }

  }

}

case class DbComponent @Inject() (
  default: Database,
  @NamedDatabase("default") named: Database,
  @NamedDatabase("other") other: Database)
