/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.NoHttpFiltersComponents;
import play.routing.Router;

// #remove-all-filters-compile-time-di
public class RemoveAllHttpFiltersComponents extends BuiltInComponentsFromContext implements NoHttpFiltersComponents {

    public RemoveAllHttpFiltersComponents(ApplicationLoader.Context context) {
        super(context);
    }

    // no need to override httpFilters method

    @Override
    public Router router() {
        // implement the router as needed
        return Router.empty();
    }
}
// #remove-all-filters-compile-time-di
