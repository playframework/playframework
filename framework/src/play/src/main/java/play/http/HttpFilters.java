/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.http;

import play.api.http.JavaHttpFiltersAdapter;
import play.mvc.EssentialFilter;

/**
 * Provides filters to the HttpRequestHandler.
 */
public interface HttpFilters {

    /**
     * Return the filters that should filter every request
     */
    EssentialFilter[] filters();

    /**
     * Get a Scala HttpFilters object
     */
    default play.api.http.HttpFilters asScala() {
        return new JavaHttpFiltersAdapter(this);
    }

}
