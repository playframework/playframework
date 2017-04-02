/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.filters.components;

import play.components.HttpComponents;
import play.mvc.EssentialFilter;

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
        CSRFComponents,
        GzipFilterComponents,
        RedirectHttpsComponents,
        SecurityHeadersComponents,
        HttpComponents {

    @Override
    default EssentialFilter[] httpFilters() {
        return new EssentialFilter[] {
            csrfFilter().asJava(),
            securityHeadersFilter().asJava(),
            allowedHostsFilter().asJava()
        };
    }
}
