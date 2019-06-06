/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.application.httpfilters;

// #filters
import play.http.DefaultHttpFilters;
import play.filters.gzip.GzipFilter;
import javax.inject.Inject;

public class Filters extends DefaultHttpFilters {
  @Inject
  public Filters(GzipFilter gzip, LoggingFilter logging) {
    super(gzip, logging);
  }
}
// #filters
