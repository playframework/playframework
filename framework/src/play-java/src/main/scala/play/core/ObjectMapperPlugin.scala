/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core

import play.api.Plugin
import play.api.Application
import play.libs.Json
import play.api.libs.json.JacksonJson
import com.fasterxml.jackson.databind.ObjectMapper

/**
 * Plugin that injects an object mapper to the JSON library on start and on stop.
 *
 * This solves the issue of the ObjectMapper cache from holding references to the application class loader between
 * reloads.
 */
class ObjectMapperPlugin(app: Application) extends Plugin {

  override def onStart() {
    Json.setObjectMapper(JacksonJson.createMapper())
  }

  override def onStop() {
    Json.setObjectMapper(null)
  }
}
