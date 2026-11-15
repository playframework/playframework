/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.Future
import scala.jdk.CollectionConverters._

import jakarta.inject._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.serialization.jackson3.JacksonObjectMapperProvider
import play.api.inject._
import play.api.libs.json.jackson.JacksonJson
import play.api.libs.json.jackson.PlayJsonMapperModule
import play.api.libs.json.BigDecimalParseConfig
import play.api.libs.json.BigDecimalSerializerConfig
import play.api.libs.json.JsonConfig
import play.api.Configuration
import play.libs.Json
import tools.jackson.databind.json.JsonMapper

/**
 * Module that injects a Jackson JsonMapper to the JSON library on start and on stop.
 *
 * This solves the issue of the JsonMapper cache from holding references to the application class loader between
 * reloads.
 */
class JsonMapperModule
    extends SimpleModule(
      bind[JsonMapper].toProvider[JsonMapperProvider].eagerly()
    )

object JsonMapperProvider {
  val BINDING_NAME = "play"
}
@Singleton
class JsonMapperProvider @Inject() (
    lifecycle: ApplicationLifecycle,
    actorSystem: ActorSystem,
    configuration: Configuration
) extends Provider[JsonMapper] {

  private val staticJsonMapperInitialized = new AtomicBoolean(false)

  lazy val get: JsonMapper = {
    val mapper = {
      // Pekko's API is typed as ObjectMapper, but it always creates a JsonMapper (unless a CBOR mapper is requested)
      val om = JacksonObjectMapperProvider
        .get(actorSystem)
        .getOrCreate(JsonMapperProvider.BINDING_NAME, Option.empty) match {
        case jsonMapper: JsonMapper => jsonMapper
        case other                  => throw new IllegalStateException(s"Expected a JsonMapper but got ${other.getClass.getName}")
      }
      if (om.registeredModules().asScala.exists(_.getModuleName == "PlayJson")) {
        om
      } else {
        val jsonConfig = JsonConfig(
          BigDecimalParseConfig(
            JsonConfig.parseMathContextValue(configuration.getOptional[String](JsonConfig.mathContextProperty)),
            configuration.getOptional[Int](JsonConfig.scaleLimitProperty).getOrElse(JsonConfig.defaultScaleLimit),
            om.tokenStreamFactory()
              .streamReadConstraints()
              .getMaxNumberLength() // we can override play-json's limit with ours
          ),
          BigDecimalSerializerConfig(
            configuration
              .getOptional[String](JsonConfig.minPlainProperty)
              .map(BigDecimal.exact(_))
              .getOrElse(JsonConfig.defaultMinPlain),
            configuration
              .getOptional[String](JsonConfig.maxPlainProperty)
              .map(BigDecimal.exact(_))
              .getOrElse(JsonConfig.defaultMaxPlain),
            configuration
              .getOptional[Boolean](JsonConfig.preserveZeroDecimalProperty)
              .getOrElse(JsonConfig.defaultPreserveZeroDecimal)
          ),
          om.tokenStreamFactory().streamReadConstraints(),
          om.tokenStreamFactory().streamWriteConstraints()
        )
        if (!staticJsonMapperInitialized.get()) {
          // Here we rely on compareAndSet(...) being called below... (a bit dirty, yes)
          JacksonJson.setConfig(jsonConfig)
        }
        // Jackson 3 mappers are immutable: build a new one that also has the PlayJson module registered
        om.rebuild[JsonMapper, JsonMapper.Builder]().addModule(new PlayJsonMapperModule(jsonConfig)).build()
      }
    }
    if (staticJsonMapperInitialized.compareAndSet(false, true)) {
      Json.setObjectMapper(mapper)

      lifecycle.addStopHook { () => Future.successful(Json.setObjectMapper(null)) }
    }
    mapper
  }
}

/**
 * Components for Jackson JsonMapper and Play's Json.
 */
trait JsonMapperComponents {
  def actorSystem: ActorSystem
  def applicationLifecycle: ApplicationLifecycle
  def configuration: Configuration

  lazy val jsonMapper: JsonMapper = new JsonMapperProvider(applicationLifecycle, actorSystem, configuration).get
}
