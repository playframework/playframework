/*
 * Copyright (C) 2009-2020 Lightbend Inc. <https://www.lightbend.com>
 */

package play.core

import akka.actor.ActorSystem
import akka.serialization.jackson.JacksonObjectMapperProvider
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
class ObjectMapperModule
    extends SimpleModule(
      bind[ObjectMapper].toProvider[ObjectMapperProvider].eagerly()
    )

@Singleton
class ObjectMapperProvider @Inject() (lifecycle: ApplicationLifecycle, actorSystem: ActorSystem)
    extends Provider[ObjectMapper] {
  private val BINDING_NAME = "play"

  lazy val get: ObjectMapper = {
    val mapper = JacksonObjectMapperProvider.get(actorSystem).getOrCreate(BINDING_NAME, Option.empty)
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
