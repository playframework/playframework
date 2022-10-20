/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.List;
import play.data.validation.Constraints;
import play.libs.Files.TemporaryFile;
import play.mvc.Http.MultipartFormData.FilePart;

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
