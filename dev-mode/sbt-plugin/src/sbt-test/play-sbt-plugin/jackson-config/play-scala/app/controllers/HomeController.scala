/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

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
    Results.Ok
  }
}
