package play.http;

import java.util.Arrays;

import play.mvc.EssentialFilter;

/**
 * Helper class which has a varargs constructor taking the filters. Reduces boilerplate for defining HttpFilters.
 */
public class DefaultHttpFilters implements HttpFilters {

  private final EssentialFilter[] filters;

  public DefaultHttpFilters(play.api.mvc.EssentialFilter... filters) {
    this.filters = Arrays.stream(filters).map(f -> f.asJava()).toArray(EssentialFilter[]::new);
  }

  @Override
  public EssentialFilter[] filters() {
    return filters;
  }
}
