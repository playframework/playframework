/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.application.httpfilters;

// #filters
import play.mvc.EssentialFilter;
import play.http.HttpFilters;
import play.filters.gzip.GzipFilter;
import javax.inject.Inject;

public class Filters implements HttpFilters {

  private final GzipFilter gzip;
  private final LoggingFilter logging;

  @Inject
  public Filters(GzipFilter gzip, LoggingFilter logging) {
    this.gzip = gzip;
    this.logging = logging;
  }

  @Override
  public EssentialFilter[] filters() {
    return new EssentialFilter[] { gzip.asJava(), logging.asJava() };
  }
}
//#filters
