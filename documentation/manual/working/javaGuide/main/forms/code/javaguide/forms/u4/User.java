/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.forms.u4;

// #user
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

public class User {

  public String email;
  public String password;
  public FilePart<TemporaryFile> profilePicture;
}
// #user
