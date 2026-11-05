/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.data;

import java.util.Date;
import play.data.format.Formats.DateTime;

public class LocalizedDate {

  // The month name depends on the locale
  @DateTime(pattern = "d MMMM yyyy")
  public Date date;
}
