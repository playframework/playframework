/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.Future

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject._
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.serialization.jackson.JacksonObjectMapperProvider
import play.api.inject._
import play.api.libs.json.jackson.JacksonJson
import play.api.libs.json.jackson.PlayJsonMapperModule
import play.api.libs.json.BigDecimalParseConfig
import play.api.libs.json.JsonConfig
import play.libs.Json

/**
 * Module that injects an object mapper to the JSON library on start and on stop.
 *
 * This solves the issue of the ObjectMapper cache from holding references to the application class loader between
 * reloads.
 */
class ObjectMapperModule
    extends SimpleModule(
      bind[ObjectMapper].toProvider[ObjectMapperProvider].eagerly()
    )

object ObjectMapperProvider {
  val BINDING_NAME = "play"
}
@Singleton
class ObjectMapperProvider @Inject() (lifecycle: ApplicationLifecycle, actorSystem: ActorSystem)
    extends Provider[ObjectMapper] {

  private val staticObjectMapperInitialized = new AtomicBoolean(false)

  lazy val get: ObjectMapper = {
    val mapper = {
      val om = JacksonObjectMapperProvider
        .get(actorSystem)
        .getOrCreate(ObjectMapperProvider.BINDING_NAME, Option.empty)
      if (om.getRegisteredModuleIds().contains("PlayJson")) {
        om
      } else {
        val jsonConfig = JsonConfig(
          BigDecimalParseConfig(
            JsonConfig.settings.bigDecimalParseConfig.mathContext,
            JsonConfig.settings.bigDecimalParseConfig.scaleLimit,
            om.getFactory().streamReadConstraints().getMaxNumberLength() // we can override play-json's limit with ours
          ),
          JsonConfig.settings.bigDecimalSerializerConfig,
          om.getFactory().streamReadConstraints(),
          om.getFactory().streamWriteConstraints()
        )
        if (!staticObjectMapperInitialized.get()) {
          // Here we rely on compareAndSet(...) being called below... (a bit dirty, yes)
          JacksonJson.setConfig(jsonConfig)
        }
        // Modifies the ObjectMapper, so we should not come back in this "else" again
        om.registerModule(new PlayJsonMapperModule(jsonConfig))
      }
    }
    if (staticObjectMapperInitialized.compareAndSet(false, true)) {
      Json.setObjectMapper(mapper)

      lifecycle.addStopHook { () => Future.successful(Json.setObjectMapper(null)) }
    }
    mapper
  }
}

/**
 * Components for Jackson ObjectMapper and Play's Json.
 */
trait ObjectMapperComponents {
  def actorSystem: ActorSystem
  def applicationLifecycle: ApplicationLifecycle

  lazy val objectMapper: ObjectMapper = new ObjectMapperProvider(applicationLifecycle, actorSystem).get
}
