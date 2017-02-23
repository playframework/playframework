/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.sbt

import com.typesafe.config.ConfigFactory
import org.specs2.mutable._

object ApplicationSecretGeneratorSpec extends Specification {
  "ApplicationSecretGenerator" should {
    "override literal secret" in {
      val configContent =
        """
          |# test configuration
          |play.crypto.secret=changeme
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.crypto.secret").should_===("newSecret")
    }

    "override nested secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  crypto {
          |    secret=changeme
          |  }
          |}
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.crypto.secret").should_===("newSecret")
    }

    "deletes existing nested application.secret while overriting secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  crypto {
          |    secret=changeme
          |  }
          |}
          |application {
          |  secret=deleteme
          |}
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.crypto.secret") must_== ("newSecret")
      newConfig.hasPath("application.secret") must beFalse
    }

    "deletes existing fixed application.secret while overriting secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  crypto {
          |    secret=changeme
          |  }
          |}
          |application.secret=deleteme
          |
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.crypto.secret") must_== ("newSecret")
      newConfig.hasPath("application.secret") must beFalse
    }
  }
}
