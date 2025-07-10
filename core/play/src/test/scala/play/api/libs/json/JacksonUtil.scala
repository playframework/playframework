/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.json

import com.fasterxml.jackson.core.StreamReadConstraints
import com.typesafe.config.ConfigFactory
import play.api.ConfigLoader
import play.api.Configuration

// temp class - will be refactored to provide a new API for parsing JSON inputs
class JacksonUtil(config: Configuration) {

  import ConfigLoader.configLoader

  def parseJsValue(input: String): JsValue = {
    val streamReadConstraints = StreamReadConstraints
      .builder()
      .maxNestingDepth(config.get[Int]("play.json.read.max-nesting-depth"))
      .maxStringLength(config.get[Int]("play.json.read.max-string-length"))
      .maxNameLength(config.get[Int]("play.json.read.max-name-length"))
      .maxNumberLength(Int.MaxValue) // play-json uses BigDecimalParseConfig instead
      .build()
    val jsonConfig = JsonConfig(
      JsonConfig.settings.bigDecimalParseConfig,
      JsonConfig.settings.bigDecimalSerializerConfig,
      streamReadConstraints
    )
    jackson.JacksonJson(jsonConfig).parseJsValue(input)
  }
}

object JacksonUtil extends JacksonUtil(new Configuration(ConfigFactory.load))
