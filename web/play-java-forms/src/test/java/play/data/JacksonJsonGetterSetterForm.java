/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import play.data.validation.Constraints;

public class JacksonJsonGetterSetterForm {

  @Constraints.Required private String authorName;

  @Constraints.Required private String bookName;

  @JsonGetter("author")
  public String getAuthorName() {
    return authorName;
  }

  @JsonSetter("AUTHOR-NAME")
  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  @JsonProperty("book_name")
  public String getBookName() {
    return bookName;
  }

  @JsonProperty("title")
  public void setBookName(String bookName) {
    this.bookName = bookName;
  }
}
