/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import play.data.validation.Constraints;
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

import java.util.List;

public class Letter {

  @Constraints.Required
  @Constraints.MinLength(10)
  private String address;

  @Constraints.Required private FilePart<TemporaryFile> coverPage;

  @Constraints.Required private List<FilePart<TemporaryFile>> letterPages;

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public FilePart<TemporaryFile> getCoverPage() {
    return coverPage;
  }

  public void setCoverPage(FilePart<TemporaryFile> coverPage) {
    this.coverPage = coverPage;
  }

  public List<FilePart<TemporaryFile>> getLetterPages() {
    return letterPages;
  }

  public void setLetterPages(List<FilePart<TemporaryFile>> letterPages) {
    this.letterPages = letterPages;
  }
}
