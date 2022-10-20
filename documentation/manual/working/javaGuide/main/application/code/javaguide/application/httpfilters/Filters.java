/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.application.httpfilters;

// #filters
import javax.inject.Inject;
import play.filters.gzip.GzipFilter;
import play.http.DefaultHttpFilters;

public class Filters extends DefaultHttpFilters {
  @Inject
  public Filters(GzipFilter gzip, LoggingFilter logging) {
    super(gzip, logging);
  }
}
// #filters
