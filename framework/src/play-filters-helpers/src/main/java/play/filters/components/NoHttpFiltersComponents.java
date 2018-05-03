/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.HttpComponents;
import play.mvc.EssentialFilter;

import java.util.Collections;
import java.util.List;

/**
 * Java component to mix in when no default filters should be mixed in to {@link play.BuiltInComponents}.
 *
 * <p>Usage:</p>
 *
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext implements NoHttpFiltersComponents {
 *
 *    public MyComponents(ApplicationLoader.Context context) {
 *        super(context);
 *    }
 *
 *    // required methods implementation
 *
 * }
 * </pre>
 *
 * @see HttpFiltersComponents#httpFilters()
 */
public interface NoHttpFiltersComponents extends HttpComponents {

    @Override
    default List<EssentialFilter> httpFilters() {
        return Collections.emptyList();
    }
}
