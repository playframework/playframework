/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import play.data.validation.Constraints;

@JsonNaming(SnakeCaseStrategy.class)
public class JacksonJsonNamingForm {

  @Constraints.Required private String authorName;

  private Integer bookCount;

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }

  public Integer getBookCount() {
    return bookCount;
  }

  public void setBookCount(Integer bookCount) {
    this.bookCount = bookCount;
  }
}
