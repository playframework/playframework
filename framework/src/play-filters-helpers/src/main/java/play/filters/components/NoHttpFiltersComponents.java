/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.components;

import play.components.HttpComponents;
import play.mvc.EssentialFilter;

/**
 * Java component to mix in when no default filters should be mixed in to {@link play.BuiltInComponents}.
 *
 * @see HttpFiltersComponents
 */
public interface NoHttpFiltersComponents extends HttpComponents {

    @Override
    default EssentialFilter[] httpFilters() {
        return new EssentialFilter[]{};
    }
}
