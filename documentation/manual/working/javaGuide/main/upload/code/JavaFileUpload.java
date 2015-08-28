import play.mvc.Controller;
import java.io.File;

public class JavaFileUpload {

    static class SyncUpload extends Controller {
        //#syncUpload
        public static play.mvc.Result upload() {
            play.mvc.Http.MultipartFormData<File> body = request().body().asMultipartFormData();
            play.mvc.Http.MultipartFormData.FilePart<File> picture = body.getFile("picture");
            if (picture != null) {
                String fileName = picture.getFilename();
                String contentType = picture.getContentType();
                java.io.File file = picture.getFile();
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
        public static play.mvc.Result upload() {
            java.io.File file = request().body().asRaw().asFile();
            return ok("File uploaded");
        }
        //#asyncUpload
    }
}
