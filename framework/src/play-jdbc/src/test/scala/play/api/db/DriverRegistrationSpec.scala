/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import java.sql.{ DriverManager, SQLException }
import org.specs2.mutable.Specification
import play.api.{ Configuration, Mode }
import scala.util.Try

object DriverRegistrationSpec extends Specification {

  "JDBC driver" should {
    sequential

    "be registered for H2 before databases start" in {
      DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull)
    }

    "not be registered for Acolyte until databases are connected" in {
      Try { // Ensure driver is not registered
        DriverManager.deregisterDriver(DriverManager.getDriver(jdbcUrl))
      }

      DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must (
        throwA[SQLException](message = "No suitable driver"))
    }

    "be registered for both Acolyte & H2 when databases are connected" in {
      dbApi.connect()

      (DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must not(beNull)).
        and(DriverManager.getDriver("jdbc:h2:mem:").
          aka("H2 driver") must not(beNull))
    }

    "be deregistered for Acolyte but still there for H2 after databases stop" in {
      dbApi.shutdown()

      (DriverManager.getDriver("jdbc:h2:mem:") aka "H2 driver" must not(beNull))
        .and(DriverManager.getDriver(jdbcUrl) aka "Acolyte driver" must {
          throwA[SQLException](message = "No suitable driver")
        })
    }
  }

  val jdbcUrl = "jdbc:acolyte:test?handler=DriverRegistrationSpec"

  lazy val dbApi: DefaultDBApi = {
    // Fake driver
    acolyte.jdbc.Driver.register("DriverRegistrationSpec", acolyte.jdbc.CompositeHandler.empty())

    new DefaultDBApi(
      Configuration.from(Map(
        "default.driver" -> "acolyte.jdbc.Driver",
        "default.url" -> jdbcUrl)))
  }
}
