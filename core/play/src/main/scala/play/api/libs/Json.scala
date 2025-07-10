/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import com.fasterxml.jackson.core.{JsonFactory, StreamReadConstraints, StreamWriteConstraints}
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import play.api.Configuration
import play.api.libs.json.{JsValue, JsonConfig}
import play.api.libs.json.jackson.JacksonJson

// TODO add scala doc for this class
class Json(config: Configuration) {

  private val streamReadConstraints = StreamReadConstraints
    .builder()
    .maxNestingDepth(config.get[Int]("play.json.read.max-nesting-depth"))
    .maxStringLength(config.get[Int]("play.json.read.max-string-length"))
    .maxNameLength(config.get[Int]("play.json.read.max-name-length"))
    .maxNumberLength(Int.MaxValue) // play-json uses BigDecimalParseConfig instead
    .build()
  private val streamWriteConstraints = StreamWriteConstraints
    .builder()
    .maxNestingDepth(config.get[Int]("play.json.write.max-nesting-depth"))
    .build()
  private val jacksonJson = {
    val jsonConfig = JsonConfig(
      JsonConfig.settings.bigDecimalParseConfig,
      JsonConfig.settings.bigDecimalSerializerConfig,
      streamReadConstraints
    )
    // JacksonJson companion object is less visible than the JacksonJson class
    // so we need to use the constructor directly here.
    new JacksonJson(jsonConfig)
  }

  def parseJsValue(input: String): JsValue = jacksonJson.parseJsValue(input)

  private[play ]def newDefaultMapper: ObjectMapper = {
    val factory = new JsonFactory()
    factory.setStreamReadConstraints(streamReadConstraints)
    factory.setStreamWriteConstraints(streamWriteConstraints)
    JsonMapper.builder(factory)
      .addModules(new Jdk8Module, new JavaTimeModule, new ParameterNamesModule, new DefaultScalaModule)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
      .build()
  }
}

object Json extends Json(new Configuration(ConfigFactory.load))
