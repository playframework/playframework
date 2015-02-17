/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.http;

import play.api.mvc.EssentialFilter;

/**
 * Provides filters to the HttpRequestHandler.
 */
public interface HttpFilters {

    /**
     * Return the filters that should filter every request
     */
    EssentialFilter[] filters();
}
