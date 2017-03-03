package play.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import play.api.http.DefaultFilters;
import play.mvc.EssentialFilter;

/**
 * Helper class which has a varargs constructor taking the filters. Reduces boilerplate for defining HttpFilters.
 */
public class DefaultHttpFilters implements HttpFilters {

  private final EssentialFilter[] filters;

  public DefaultHttpFilters(DefaultFilters defaultFilters, play.api.mvc.EssentialFilter... filters) {
    EssentialFilter[] defaults = Optional.ofNullable(defaultFilters).map(f -> f.asJava().filters()).orElseGet(() -> new EssentialFilter[0]);

    EssentialFilter[] userFilters = Arrays.stream(filters).map(f -> f.asJava()).toArray(EssentialFilter[]::new);

    this.filters = Stream.concat(Arrays.stream(defaults), Arrays.stream(userFilters)).toArray(EssentialFilter[]::new);
  }

  /**
   * @deprecated Use the version that uses DefaultFilters
   * @param filters the list of essential filters
   */
  @Deprecated
  public DefaultHttpFilters(play.api.mvc.EssentialFilter... filters) {
    this(null, filters);
  }

  @Override
  public EssentialFilter[] filters() {
    return filters;
  }
}
