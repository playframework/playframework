/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.sbt

import com.typesafe.config.ConfigFactory
import org.specs2.mutable._

class ApplicationSecretGeneratorSpec extends Specification {
  "ApplicationSecretGenerator" should {
    "override literal secret" in {
      val configContent =
        """
          |# test configuration
          |play.http.secret.key=changeme
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.http.secret.key").should_===("newSecret")
    }

    "override nested secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  http {
          |    secret {
          |      key=changeme
          |    }
          |  }
          |}
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.http.secret.key").should_===("newSecret")
    }

    "deletes existing nested play.crypto.secret while overwriting secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  http {
          |    secret {
          |      key=changeme
          |    }
          |  }
          |}
          |play {
          |  crypto {
          |    secret=deleteme
          |  }
          |}
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.http.secret.key") must_== "newSecret"
      newConfig.hasPath("play.crypto.secret") must beFalse
    }

    "deletes existing fixed play.crypto.secret while overwriting secret" in {
      val configContent =
        """
          |# test configuration
          |play {
          |  http {
          |    secret {
          |      key=changeme
          |    }
          |  }
          |}
          |play.crypto.secret=deleteme
          |
          |""".stripMargin
      val config = ConfigFactory.parseString(configContent)
      val lines = configContent.split("\n").toList
      val newLines: List[String] = ApplicationSecretGenerator.getUpdatedSecretLines("newSecret", lines, config)

      val newConfig = ConfigFactory.parseString(newLines.mkString("\n"))
      newConfig.getString("play.http.secret.key") must_== "newSecret"
      newConfig.hasPath("play.crypto.secret") must beFalse
    }
  }
}
