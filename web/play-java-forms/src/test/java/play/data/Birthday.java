/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.Date;

public class Birthday {

  @play.data.format.Formats.DateTime(pattern = "customFormats.date")
  private Date date;

  // No annotation
  private Date alternativeDate;

  public Date getDate() {
    return this.date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Date getAlternativeDate() {
    return this.alternativeDate;
  }

  public void setAlternativeDate(Date date) {
    this.alternativeDate = date;
  }
}
