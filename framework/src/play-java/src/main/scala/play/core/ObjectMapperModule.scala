/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import com.fasterxml.jackson.databind.ObjectMapper
import play.api.inject._
import play.libs.Json

import javax.inject._

import scala.concurrent.Future

/**
 * Module that injects an object mapper to the JSON library on start and on stop.
 *
 * This solves the issue of the ObjectMapper cache from holding references to the application class loader between
 * reloads.
 */
class ObjectMapperModule extends SimpleModule(
  bind[ObjectMapper].toProvider[ObjectMapperProvider].eagerly()
)

@Singleton
class ObjectMapperProvider @Inject() (lifecycle: ApplicationLifecycle) extends Provider[ObjectMapper] {
  lazy val get: ObjectMapper = {
    val objectMapper = Json.newDefaultMapper()
    Json.setObjectMapper(objectMapper)
    lifecycle.addStopHook { () =>
      Future.successful(Json.setObjectMapper(null))
    }
    objectMapper
  }
}

/**
 * Components for Jackson ObjectMapper and Play's Json.
 */
trait ObjectMapperComponents {

  def applicationLifecycle: ApplicationLifecycle

  lazy val objectMapper: ObjectMapper = new ObjectMapperProvider(applicationLifecycle).get
}