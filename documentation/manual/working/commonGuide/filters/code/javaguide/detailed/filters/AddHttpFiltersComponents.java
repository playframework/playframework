/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters;

import javaguide.application.httpfilters.LoggingFilter;

// #appending-filters-compile-time-di
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.HttpFiltersComponents;
import play.mvc.EssentialFilter;
import play.routing.Router;

import java.util.Arrays;
import java.util.List;

public class AddHttpFiltersComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {

    public AddHttpFiltersComponents(ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public EssentialFilter[] httpFilters() {
        List<EssentialFilter> combinedFilters = Arrays.asList(HttpFiltersComponents.super.httpFilters());
        combinedFilters.add(new LoggingFilter(materializer()));

        EssentialFilter[] activeFilters = new EssentialFilter[combinedFilters.size()];
        return combinedFilters.toArray(activeFilters);
    }

    @Override
    public Router router() {
        // implement the router as needed
        return Router.empty();
    }
}
// #appending-filters-compile-time-di
