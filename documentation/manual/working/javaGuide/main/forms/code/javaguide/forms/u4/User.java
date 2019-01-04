/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.u4;

//#user
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

public class User {

    public String email;
    public String password;
    public FilePart<TemporaryFile> profilePicture;

}
//#user
