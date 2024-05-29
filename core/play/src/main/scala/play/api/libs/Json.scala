/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.libs

import java.io.InputStream

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.core.StreamWriteConstraints
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.typesafe.config.ConfigFactory
import org.apache.pekko.serialization.jackson.PekkoJacksonModule
import org.apache.pekko.serialization.jackson.PekkoStreamJacksonModule
import org.apache.pekko.serialization.jackson.PekkoTypedJacksonModule
import play.api.libs.json.jackson.JacksonJson
import play.api.libs.json.JsValue
import play.api.libs.json.JsonConfig
import play.api.Configuration

/**
 * Internal class for Play JSON parsing.
 * <p>
 *   Only for internal use.
 * </p>
 */
private[play] class Json(config: Configuration) {

  private val streamReadConstraints = StreamReadConstraints
    .builder()
    .maxNestingDepth(config.get[Int]("play.json.read.max-nesting-depth"))
    .maxStringLength(config.get[Int]("play.json.read.max-string-length"))
    .maxNameLength(config.get[Int]("play.json.read.max-name-length"))
    .maxNumberLength(1000) // play-json uses BigDecimalParseConfig instead
    .build()
  private val streamWriteConstraints = StreamWriteConstraints
    .builder()
    .maxNestingDepth(config.get[Int]("play.json.write.max-nesting-depth"))
    .build()
  private val jacksonJson = {
    val jsonConfig = JsonConfig(
      JsonConfig.settings.bigDecimalParseConfig,
      JsonConfig.settings.bigDecimalSerializerConfig,
      streamReadConstraints,
      streamWriteConstraints,
    )
    // JacksonJson companion object is less visible than the JacksonJson class
    // so we need to use the constructor directly here.
    new JacksonJson(jsonConfig)
  }

  def parse(input: String): JsValue = jacksonJson.parseJsValue(input)

  def parse(input: InputStream): JsValue = jacksonJson.parseJsValue(input)

  def stringify(value: JsValue): String = jacksonJson.generateFromJsValue(value, false)

  def toBytes(value: JsValue): Array[Byte] = jacksonJson.jsValueToBytes(value)

  /**
   * Internal method to create a new default ObjectMapper.
   * <p>
   *   Only for internal use.
   * </p>
   * @return a new ObjectMapper instance configured with the default settings
   */
  private[play] def newDefaultMapper: ObjectMapper = {
    val factory = new JsonFactory()
    factory.setStreamReadConstraints(streamReadConstraints)
    factory.setStreamWriteConstraints(streamWriteConstraints)
    JsonMapper
      .builder(factory)
      .addModules(
        new PekkoJacksonModule,
        new PekkoTypedJacksonModule,
        new PekkoStreamJacksonModule,
        new ParameterNamesModule,
        new Jdk8Module,
        new JavaTimeModule,
        new DefaultScalaModule
      )
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
      .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
      .build()
  }
}

/**
 * Internal class for Play JSON parsing.
 * <p>
 *   Only for internal use.
 * </p>
 */
private[play] object Json extends Json(new Configuration(ConfigFactory.load))
