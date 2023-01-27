/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
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
