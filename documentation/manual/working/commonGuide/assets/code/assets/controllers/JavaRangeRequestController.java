/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package assets.controllers;

import java.io.File;
import play.mvc.*;

public class JavaRangeRequestController extends Controller {

  // #range-request
  public Result video(Http.Request request, Long videoId) {
    File videoFile = getVideoFile(videoId);
    return RangeResults.ofFile(request, videoFile);
  }
  // #range-request

  private File getVideoFile(Long videoId) {
    return new File("video.mp4");
  }
}
