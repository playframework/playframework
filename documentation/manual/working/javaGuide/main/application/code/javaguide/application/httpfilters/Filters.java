package javaguide.application.httpfilters;

// #filters
import play.api.mvc.EssentialFilter;
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
    return new EssentialFilter[] { gzip, logging };
  }
}
//#filters
