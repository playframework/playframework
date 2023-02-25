/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package assets.controllers

import java.io.File
import javax.inject.Inject

import play.api.mvc._

class RangeRequestController @Inject() (c: ControllerComponents) extends AbstractController(c) {
  // #range-request
  def video(videoId: Long): Action[AnyContent] = Action { implicit request =>
    val videoFile = getVideoFile(videoId)
    RangeResult.ofFile(videoFile, request.headers.get(RANGE), Some("video/mp4"))
  }
  // #range-request

  private def getVideoFile(videoId: Long) = new File("video.mp4")
}
