/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.http;

import play.api.http.JavaHttpFiltersAdapter;
import play.mvc.EssentialFilter;

/**
 * Provides filters to the HttpRequestHandler.
 */
public interface HttpFilters {

    /**
     * @return the filters that should filter every request
     */
    EssentialFilter[] filters();

    /**
     * @return a Scala HttpFilters object
     */
    default play.api.http.HttpFilters asScala() {
        return new JavaHttpFiltersAdapter(this);
    }

}
