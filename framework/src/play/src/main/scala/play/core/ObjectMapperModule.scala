/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.core

import javax.inject._

import com.fasterxml.jackson.databind.{ ObjectMapper, ObjectReader, ObjectWriter }
import play.api.inject._
import play.api.{ Configuration, Environment }
import play.libs.Json
import play.utils.Reflect

import scala.concurrent.Future

/**
 * Module that injects an object mapper to the JSON library on start and on stop.
 *
 * This solves the issue of the ObjectMapper cache from holding references to the application class loader between
 * reloads.
 */
class ObjectMapperModule extends SimpleModule((env: Environment, conf: Configuration) => {
  val providerClassName = conf.get[String]("play.objectMapper.provider")
  val providerClass = Reflect.getClass[play.libs.ObjectMapperProvider](providerClassName, env.classLoader)
  Seq(
    bind[ObjectMapper].toProvider(providerClass).in[Singleton],
    bind[ObjectWriter].toProvider[ObjectWriterProvider].in[Singleton],
    bind[ObjectReader].toProvider[ObjectReaderProvider].in[Singleton],
    bind[ObjectMapperSetter].toSelf.eagerly()
  )
})

/**
 * This class is only needed to set the global state used by play.libs.Json.
 */
@Singleton
class ObjectMapperSetter @Inject() (objectMapper: ObjectMapper, lifecycle: ApplicationLifecycle) {
  Json.setObjectMapper(objectMapper)
  lifecycle.addStopHook { () =>
    Future.successful(Json.setObjectMapper(null))
  }
}

class ObjectWriterProvider @Inject() (mapper: ObjectMapper) extends Provider[ObjectWriter] {
  def get = mapper.writer
}

class ObjectReaderProvider @Inject() (mapper: ObjectMapper) extends Provider[ObjectReader] {
  def get = mapper.reader
}
