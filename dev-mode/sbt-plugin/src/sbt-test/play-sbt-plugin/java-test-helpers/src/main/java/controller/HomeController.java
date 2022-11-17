/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers;

import play.libs.Files;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import java.io.IOException;

public class HomeController extends Controller {

    public Result multipartFormUploadNoFiles(Http.Request request) {
        Http.MultipartFormData<Files.TemporaryFile> body = request.body().asMultipartFormData();
        return ok("Files: " + body.getFiles().size() + ", Data: " + body.asFormUrlEncoded().size() + " [" + body.asFormUrlEncoded().get("key1")[0]  +  "]");
    }

    public Result multipartFormUpload(Http.Request request) throws IOException {
        Http.MultipartFormData<Object> data =
                request.body().asMultipartFormData();
        Files.TemporaryFile ref =
                (Files.TemporaryFile) data.getFile("document").getRef();
        String contents = java.nio.file.Files.readString(ref.path());
        return ok(
                "author: "
                        + data.asFormUrlEncoded().get("author")[0]
                        + "\n"
                        + "filename: "
                        + data.getFile("document").getFilename()
                        + "\n"
                        + "contentType: "
                        + data.getFile("document").getContentType()
                        + "\n"
                        + "contents: "
                        + contents
                        + "\n");
    }

}