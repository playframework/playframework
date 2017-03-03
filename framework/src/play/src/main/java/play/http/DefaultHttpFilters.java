package play.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import play.api.http.EnabledFilters;
import play.mvc.EssentialFilter;

/**
 * Helper class for defining HttpFilters.
 */
public class DefaultHttpFilters implements HttpFilters {

  private final EssentialFilter[] filters;

  /**
   * Creates a list of filters from the two inputs filter lists.
   *
   * @param enabledFilters the list of filters from `play.filters.enabled`
   * @param filters The user defined list of filters.
   */
  public DefaultHttpFilters(EnabledFilters enabledFilters, play.api.mvc.EssentialFilter... filters) {
    EssentialFilter[] defaults = Optional.ofNullable(enabledFilters).map(f -> f.asJava().filters()).orElseGet(() -> new EssentialFilter[0]);

    EssentialFilter[] userFilters = Arrays.stream(filters).map(f -> f.asJava()).toArray(EssentialFilter[]::new);

    this.filters = Stream.concat(Arrays.stream(defaults), Arrays.stream(userFilters)).toArray(EssentialFilter[]::new);
  }

  /**
   * @deprecated Use the version that uses EnabledFilters
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
