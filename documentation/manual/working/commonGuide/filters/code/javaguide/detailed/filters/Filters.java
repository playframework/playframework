/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package javaguide.detailed.filters;

// #filters-combine-enabled-filters
import play.api.http.EnabledFilters;
import play.filters.cors.CORSFilter;
import play.http.DefaultHttpFilters;
import play.mvc.EssentialFilter;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

public class Filters extends DefaultHttpFilters {

    @Inject
    public Filters(EnabledFilters enabledFilters, CORSFilter corsFilter) {
        super(combine(enabledFilters.asJava().filters(), corsFilter.asJava()));
    }

    private static EssentialFilter[] combine(EssentialFilter[] filters, EssentialFilter toAppend) {
        List<EssentialFilter> combinedFilters = Arrays.asList(filters);
        combinedFilters.add(toAppend);

        EssentialFilter[] activeFilters = new EssentialFilter[combinedFilters.size()];
        return combinedFilters.toArray(activeFilters);
    }
}
// #filters-combine-enabled-filters
