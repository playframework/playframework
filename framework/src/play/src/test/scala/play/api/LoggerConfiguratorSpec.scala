/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

import org.specs2.mutable.Specification

class LoggerConfiguratorSpec extends Specification {

  private lazy val referenceConfig = Configuration.reference

  "generateProperties" should {

    "generate in the simplest case" in {
      val env = Environment.simple()
      val config = referenceConfig
      val properties = LoggerConfigurator.generateProperties(env, config, Map.empty)
      properties.size must beEqualTo(1)
      properties must havePair("application.home" -> env.rootPath.getAbsolutePath)
    }

    "generate in the case of including string config property" in {
      val env = Environment.simple()
      val config = referenceConfig ++ Configuration(
        "play.logger.includeConfigProperties" -> true,
        "my.string.in.application.conf" -> "hello"
      )
      val properties = LoggerConfigurator.generateProperties(env, config, Map.empty)
      properties must havePair("my.string.in.application.conf" -> "hello")
    }

    "generate in the case of including integer config property" in {
      val env = Environment.simple()
      val config = referenceConfig ++ Configuration(
        "play.logger.includeConfigProperties" -> true,
        "my.number.in.application.conf" -> 1
      )
      val properties = LoggerConfigurator.generateProperties(env, config, Map.empty)
      properties must havePair("my.number.in.application.conf" -> "1")
    }

    "generate in the case of including null config property" in {
      val env = Environment.simple()
      val config = referenceConfig ++ Configuration(
        "play.logger.includeConfigProperties" -> true,
        "my.null.in.application.conf" -> null
      )
      val properties = LoggerConfigurator.generateProperties(env, config, Map.empty)
      // nulls are excluded, you must specify them directly
      // https://typesafehub.github.io/config/latest/api/com/typesafe/config/Config.html#entrySet--
      properties must not haveKey ("my.null.in.application.conf")
    }

    "generate in the case of direct properties" in {
      val env = Environment.simple()
      val config = referenceConfig
      val optProperties = Map("direct.map.property" -> "goodbye")
      val properties = LoggerConfigurator.generateProperties(env, config, optProperties)

      properties.size must beEqualTo(2)
      properties must havePair("application.home" -> env.rootPath.getAbsolutePath)
      properties must havePair("direct.map.property" -> "goodbye")
    }

    "generate a null using direct properties" in {
      val env = Environment.simple()
      val config = referenceConfig
      val optProperties = Map("direct.null.property" -> null)
      val properties = LoggerConfigurator.generateProperties(env, config, optProperties)

      properties must havePair("direct.null.property" -> null)
    }

    "override config property with direct properties" in {
      val env = Environment.simple()
      val config = referenceConfig ++ Configuration("some.property" -> "AAA")
      val optProperties = Map("some.property" -> "BBB")
      val properties = LoggerConfigurator.generateProperties(env, config, optProperties)

      properties must havePair("some.property" -> "BBB")
    }

    "generate empty properties when configuration is empty" in {
      val env = Environment.simple()
      val config = Configuration.empty
      val properties = LoggerConfigurator.generateProperties(env, config, Map.empty)
      properties must size(1)
    }

  }

}
