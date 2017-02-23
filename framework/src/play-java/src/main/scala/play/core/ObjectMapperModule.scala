/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core

import com.fasterxml.jackson.databind.ObjectMapper
import play.api._
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
class ObjectMapperModule extends Module {

  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[ObjectMapper].toProvider[ObjectMapperProvider].eagerly()
  )
}

@Singleton
class ObjectMapperProvider @Inject() (lifecycle: ApplicationLifecycle) extends Provider[ObjectMapper] {
  lazy val get = {
    val objectMapper = Json.newDefaultMapper()
    Json.setObjectMapper(objectMapper)
    lifecycle.addStopHook { () =>
      Future.successful(Json.setObjectMapper(null))
    }
    objectMapper
  }
}
