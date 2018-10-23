/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package assets.controllers;

import play.mvc.*;

import java.io.File;

public class JavaRangeRequestController extends Controller {

    private FileMimeTypes fileMimeTypes = null;

    // #range-request
    public Result video(Http.Request request, Long videoId) {
        File videoFile = getVideoFile(videoId);
        return RangeResults.ofFile(request, this.fileMimeTypes, videoFile);
    }
    // #range-request

    private File getVideoFile(Long videoId) {
        return new File("video.mp4");
    }
}
