/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.upload.fileupload.controllers;

//#syncUpload
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.nio.file.Paths;

public class HomeController extends Controller {

    public Result upload(Http.Request request) {
        Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
        Http.MultipartFormData.FilePart<TemporaryFile> picture = body.getFile("picture");
        if (picture != null) {
            String fileName = picture.getFilename();
            String contentType = picture.getContentType();
            TemporaryFile file = picture.getRef();
            file.copyTo(Paths.get("/tmp/picture/destination.jpg"), true);
            return ok("File uploaded");
        } else {
            return badRequest().flashing("error", "Missing file");
        }
    }
}
//#syncUpload
