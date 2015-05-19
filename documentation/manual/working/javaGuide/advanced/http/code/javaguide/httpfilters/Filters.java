package httpfilters;

// #filters
import play.api.Logger;
import play.api.mvc.EssentialFilter;
import play.http.HttpFilters;
import play.filters.gzip.GzipFilter;
import javax.inject.Inject;

public class Filters implements HttpFilters {

  private final GzipFilter gzip;

  @Inject
  public Filters(GzipFilter gzip) {
    this.gzip = gzip;
  }

  @Override
  public EssentialFilter[] filters() {
    return new EssentialFilter[] { gzip };
  }
}
//#filters
