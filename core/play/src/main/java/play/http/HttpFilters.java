/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import java.util.List;
import play.api.http.JavaHttpFiltersAdapter;
import play.mvc.EssentialFilter;

/** Provides filters to the HttpRequestHandler. */
public interface HttpFilters {

  /**
   * @return the list of filters that should filter every request.
   */
  List<EssentialFilter> getFilters();

  /**
   * @return a Scala HttpFilters object
   */
  default play.api.http.HttpFilters asScala() {
    return new JavaHttpFiltersAdapter(this);
  }
}
