/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import play.components.HttpComponents;
import play.mvc.EssentialFilter;

import java.util.Arrays;
import java.util.List;

/**
 * A compile time default filters components.
 *
 * <p>Usage:</p>
 *
 * <pre>
 * public class MyComponents extends BuiltInComponentsFromContext
 *                           implements play.filters.components.HttpFiltersComponents {
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
 * @see NoHttpFiltersComponents
 */
public interface HttpFiltersComponents extends
        AllowedHostsComponents,
        CORSComponents,
        CSPComponents,
        CSRFComponents,
        GzipFilterComponents,
        RedirectHttpsComponents,
        SecurityHeadersComponents,
        HttpComponents {

    @Override
    default List<EssentialFilter> httpFilters() {
        return Arrays.asList(
            csrfFilter().asJava(),
            securityHeadersFilter().asJava(),
            allowedHostsFilter().asJava()
        );
    }
}
