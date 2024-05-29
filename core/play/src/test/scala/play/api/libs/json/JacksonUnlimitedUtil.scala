/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs.json

import com.fasterxml.jackson.core.StreamReadConstraints

object JacksonUnlimitedUtil {
  def parse(input: String) = {
    val streamReadConstraints = StreamReadConstraints
      .builder()
      .maxNestingDepth(Int.MaxValue)
      .maxStringLength(Int.MaxValue)
      .maxNumberLength(Int.MaxValue)
      .build()
    val config = JsonConfig(
      JsonConfig.settings.bigDecimalParseConfig,
      JsonConfig.settings.bigDecimalSerializerConfig,
      streamReadConstraints
    )
    jackson.JacksonJson(config).parseJsValue(input)
  }
}
