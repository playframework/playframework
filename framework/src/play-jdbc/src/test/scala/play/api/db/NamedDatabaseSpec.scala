/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
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
      app.injector.instanceOf[DBApi].databases must have size (2)
      app.injector.instanceOf[DefaultComponent].db.url must_== "jdbc:h2:mem:default"
      app.injector.instanceOf[NamedDefaultComponent].db.url must_== "jdbc:h2:mem:default"
      app.injector.instanceOf[NamedOtherComponent].db.url must_== "jdbc:h2:mem:other"
    }

    "not bind default databases without configuration" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      app.injector.instanceOf[DBApi].databases must have size (1)
      app.injector.instanceOf[DefaultComponent] must throwA[com.google.inject.ConfigurationException]
      app.injector.instanceOf[NamedDefaultComponent] must throwA[com.google.inject.ConfigurationException]
      app.injector.instanceOf[NamedOtherComponent].db.url must_== "jdbc:h2:mem:other"
    }

    "not bind databases without configuration" in new WithApplication(FakeApplication()) {
      app.injector.instanceOf[DBApi].databases must beEmpty
      app.injector.instanceOf[DefaultComponent] must throwA[com.google.inject.ConfigurationException]
      app.injector.instanceOf[NamedDefaultComponent] must throwA[com.google.inject.ConfigurationException]
      app.injector.instanceOf[NamedOtherComponent] must throwA[com.google.inject.ConfigurationException]
    }

    "allow default database name to be configured" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "play.db.default" -> "other",
        "db.other.driver" -> "org.h2.Driver",
        "db.other.url" -> "jdbc:h2:mem:other"
      )
    )) {
      app.injector.instanceOf[DBApi].databases must have size (1)
      app.injector.instanceOf[DefaultComponent].db.url must_== "jdbc:h2:mem:other"
      app.injector.instanceOf[NamedOtherComponent].db.url must_== "jdbc:h2:mem:other"
      app.injector.instanceOf[NamedDefaultComponent] must throwA[com.google.inject.ConfigurationException]
    }

    "allow db config key to be configured" in new WithApplication(FakeApplication(
      additionalConfiguration = Map(
        "play.db.config" -> "databases",
        "databases.default.driver" -> "org.h2.Driver",
        "databases.default.url" -> "jdbc:h2:mem:default"
      )
    )) {
      app.injector.instanceOf[DBApi].databases must have size (1)
      app.injector.instanceOf[DefaultComponent].db.url must_== "jdbc:h2:mem:default"
      app.injector.instanceOf[NamedDefaultComponent].db.url must_== "jdbc:h2:mem:default"
    }

  }

}

case class DefaultComponent @Inject() (db: Database)

case class NamedDefaultComponent @Inject() (@NamedDatabase("default") db: Database)

case class NamedOtherComponent @Inject() (@NamedDatabase("other") db: Database)
