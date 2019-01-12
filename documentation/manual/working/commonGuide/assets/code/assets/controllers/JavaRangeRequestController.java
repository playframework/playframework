/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package assets.controllers;

import play.mvc.*;

import java.io.File;

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
