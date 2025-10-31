/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

// Using the play package here, so we are able to access play.api.libs.json.jackson.JacksonJson which private[play]
// In the Java HomeController we can use JacksonJson$.MODULE$ however.
package play.controllers

import com.fasterxml.jackson.databind.ObjectMapper

import jakarta.inject.Inject
import jakarta.inject.Singleton
import play.api.mvc._
import utils.ObjectMapperConfigUtil

import java.io.File

@Singleton
class HomeController @Inject()(val controllerComponents: ControllerComponents, val mapper: ObjectMapper) extends BaseController {

  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-scala_injected-mapper.json"), ObjectMapperConfigUtil.toConfigJson(mapper))
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-scala_play-libs-mapper.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.Json.mapper()))
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-scala_play-libs-newDefaultMapper.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.Json.newDefaultMapper()));
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-scala_play-api-libs-json-jackson-JacksonJson-mapper.json"), ObjectMapperConfigUtil.toConfigJson(play.api.libs.json.jackson.JacksonJson.get.mapper()));
    mapper.writerWithDefaultPrettyPrinter().writeValue(new File("play-scala_play-libs-ws-DefaultObjectMapper-instance.json"), ObjectMapperConfigUtil.toConfigJson(play.libs.ws.DefaultObjectMapper.instance()));
    Results.Ok
  }
}
