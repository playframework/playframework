/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.upload.fileupload.controllers;

// #syncUpload
import java.nio.file.Paths;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

public class HomeController extends Controller {

  public Result upload(Http.Request request) {
    Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
    Http.MultipartFormData.FilePart<TemporaryFile> picture = body.getFile("picture");
    if (picture != null) {
      String fileName = picture.getFilename();
      long fileSize = picture.getFileSize();
      String contentType = picture.getContentType();
      TemporaryFile file = picture.getRef();
      file.copyTo(Paths.get("/tmp/picture/destination.jpg"), true);
      return ok("File uploaded");
    } else {
      return badRequest().flashing("error", "Missing file");
    }
  }
}
// #syncUpload
