/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import scala.concurrent.Future
import akka.actor.ActorSystem
import akka.serialization.jackson.JacksonObjectMapperProvider
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import javax.inject._
import play.api.inject._
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

  lazy val get: ObjectMapper = {
    val mapper =
      JacksonObjectMapperProvider
        .get(actorSystem)
        .getOrCreate(ObjectMapperProvider.BINDING_NAME, Option.empty)
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.PUBLIC_ONLY)
    Json.setObjectMapper(mapper)

    lifecycle.addStopHook { () =>
      Future.successful(Json.setObjectMapper(null))
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
