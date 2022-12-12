/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.filters.components;

import java.util.Arrays;
import java.util.List;
import play.components.HttpComponents;
import play.mvc.EssentialFilter;

/**
 * A compile time default filters components.
 *
 * <p>Usage:
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
public interface HttpFiltersComponents
    extends AllowedHostsComponents,
        CORSComponents,
        CSPComponents,
        CSRFComponents,
        GzipFilterComponents,
        RedirectHttpsComponents,
        SecurityHeadersComponents,
        IPFilterComponents,
        HttpComponents {

  @Override
  default List<EssentialFilter> httpFilters() {
    return Arrays.asList(
        ipFilter().asJava(),
        csrfFilter().asJava(),
        securityHeadersFilter().asJava(),
        allowedHostsFilter().asJava());
  }
}
