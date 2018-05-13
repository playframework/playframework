/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import play.mvc.EssentialFilter;

/**
 * Helper class which has a varargs constructor taking the filters. Reduces boilerplate for defining HttpFilters.
 */
public class DefaultHttpFilters implements HttpFilters {

  private final List<EssentialFilter> filters;

  public DefaultHttpFilters(play.api.mvc.EssentialFilter... filters) {
    this(Arrays.asList(filters));
  }

  public DefaultHttpFilters(List<? extends play.api.mvc.EssentialFilter> filters) {
    this.filters = filters.stream().map(f -> f.asJava()).collect(Collectors.toList());
  }

  @Override
  public List<EssentialFilter> getFilters() {
    return filters;
  }
}
