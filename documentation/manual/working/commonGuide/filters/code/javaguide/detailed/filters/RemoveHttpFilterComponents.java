/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.HttpFiltersComponents;
import play.filters.csrf.CSRFFilter;
import play.mvc.EssentialFilter;
import play.routing.Router;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// #removing-filters-compile-time-di
public class RemoveHttpFilterComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {

    public RemoveHttpFilterComponents(ApplicationLoader.Context context) {
        super(context);
    }

    @Override
    public EssentialFilter[] httpFilters() {
        List<EssentialFilter> filters = Arrays
                .stream(HttpFiltersComponents.super.httpFilters())
                .filter(filter -> !filter.getClass().equals(CSRFFilter.class))
                .collect(Collectors.toList());

        EssentialFilter[] activeFilters = new EssentialFilter[filters.size()];
        return filters.toArray(activeFilters);
    }

    @Override
    public Router router() {
        // implement the router as needed
        return Router.empty();
    }
}
// #removing-filters-compile-time-di
