/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.http;

import play.api.http.JavaHttpFiltersAdapter;
import play.mvc.EssentialFilter;

import java.util.List;

/**
 * Provides filters to the HttpRequestHandler.
 */
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
