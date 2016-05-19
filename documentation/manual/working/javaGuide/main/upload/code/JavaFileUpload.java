/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
import play.mvc.Controller;
import java.io.File;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

public class JavaFileUpload {

    static class SyncUpload extends Controller {
        //#syncUpload
        public Result upload() {
            MultipartFormData<File> body = request().body().asMultipartFormData();
            FilePart<File> picture = body.getFile("picture");
            if (picture != null) {
                String fileName = picture.getFilename();
                String contentType = picture.getContentType();
                File file = picture.getFile();
                return ok("File uploaded");
            } else {
                flash("error", "Missing file");
                return badRequest();
            }
        }
        //#syncUpload
    }

    static class AsyncUpload extends Controller {
        //#asyncUpload
        public Result upload() {
            File file = request().body().asRaw().asFile();
            return ok("File uploaded");
        }
        //#asyncUpload
    }
}
