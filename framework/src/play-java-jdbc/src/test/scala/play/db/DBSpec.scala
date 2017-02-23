/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.db

import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeApplication

object DBSpec extends org.specs2.mutable.Specification {

  title("Java DB utility")

  "DB" should {
    "execute block with default connection" in {
      val id = s"withConnection-${System.identityHashCode(this)}"

      DB.withConnection(callable(id), fakeApp).
        aka("connection block result") must_== id

    }

    "execute block with connection from specified datasource" in {
      val id = s"withConnection-${System.identityHashCode(this)}"

      DB.withConnection("default", callable(id), fakeApp).
        aka("connection block result") must_== id

    }

    "execute block with transaction for default connection" in {
      val id = s"withConnection-${System.identityHashCode(this)}"

      DB.withTransaction(callable(id), fakeApp).
        aka("transaction block result") must_== id

    }

    "execute block with transaction from specified datasource" in {
      val id = s"withConnection-${System.identityHashCode(this)}"

      DB.withTransaction("default", callable(id), fakeApp).
        aka("connection block result") must_== id

    }
  }

  // ---

  def callable(res: String) = new ConnectionCallable[String] {
    def call(con: java.sql.Connection) = res
  }

  lazy val fakeApp = {
    acolyte.jdbc.Driver.register("test", acolyte.jdbc.CompositeHandler.empty())
    GuiceApplicationBuilder().configure(
      "db.default.driver" -> "acolyte.jdbc.Driver",
      "db.default.url" -> "jdbc:acolyte:test?handler=test"
    ).build()
  }
}
