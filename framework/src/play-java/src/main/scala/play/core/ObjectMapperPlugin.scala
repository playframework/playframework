/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import play.api.Plugin
import play.libs.Json
import play.api.libs.json.JacksonJson

import javax.inject.Singleton

/**
 * Plugin that injects an object mapper to the JSON library on start and on stop.
 *
 * This solves the issue of the ObjectMapper cache from holding references to the application class loader between
 * reloads.
 */
@Singleton
class ObjectMapperPlugin extends Plugin {

  override def onStart() {
    Json.setObjectMapper(JacksonJson.createMapper())
  }

  override def onStop() {
    Json.setObjectMapper(null)
  }
}
