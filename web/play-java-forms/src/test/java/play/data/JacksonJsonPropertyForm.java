/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import play.data.validation.Constraints;

public class JacksonJsonPropertyForm {

  @Constraints.Required
  @JsonProperty("AuthorName")
  private String authorName;

  public String getAuthorName() {
    return authorName;
  }

  public void setAuthorName(String authorName) {
    this.authorName = authorName;
  }
}
