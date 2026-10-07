/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import static com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy;

import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import play.data.validation.Constraints;

@JsonNaming(SnakeCaseStrategy.class)
public class JacksonJsonNestedForm {

  @Valid private PublisherDetails publisherDetails;

  public PublisherDetails getPublisherDetails() {
    return publisherDetails;
  }

  public void setPublisherDetails(PublisherDetails publisherDetails) {
    this.publisherDetails = publisherDetails;
  }

  @JsonNaming(SnakeCaseStrategy.class)
  public static class PublisherDetails {

    @Constraints.Required private String companyName;

    public String getCompanyName() {
      return companyName;
    }

    public void setCompanyName(String companyName) {
      this.companyName = companyName;
    }
  }
}
