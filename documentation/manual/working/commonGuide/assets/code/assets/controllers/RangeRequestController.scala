/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package assets.controllers

import java.io.File
import javax.inject.Inject

import play.api.mvc._

class RangeRequestController @Inject()(c: ControllerComponents) extends AbstractController(c) {

  // #range-request
  def video(videoId: Long) = Action { implicit request =>
    val videoFile = getVideoFile(videoId)
    RangeResult.ofFile(videoFile, request.headers.get(RANGE), Some("video/mp4"))
  }
  // #range-request

  private def getVideoFile(videoId: Long) = new File("video.mp4")
}
