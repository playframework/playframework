/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.u1;

//#user
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

public class User {

    protected String email;
    protected String password;
    protected FilePart<TemporaryFile> profilePicture;

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public FilePart<TemporaryFile> getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(FilePart<TemporaryFile> pic) {
        this.profilePicture = pic;
    }

}
//#user
