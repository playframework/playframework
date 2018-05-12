/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.components;

import play.core.j.JavaHandlerComponents;
import play.http.ActionCreator;
import play.http.HttpRequestHandler;
import play.mvc.EssentialFilter;

import java.util.List;

public interface HttpComponents extends HttpConfigurationComponents {

    ActionCreator actionCreator();

    /**
     * List of filters, typically provided by mixing in play.filters.HttpFiltersComponents
     * or play.api.NoHttpFiltersComponents.
     *
     * In most cases you will want to mixin HttpFiltersComponents and append your own filters:
     *
     * <pre>
     * public class MyComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {
     *
     *   public MyComponents(ApplicationLoader.Context context) {
     *       super(context);
     *   }
     *
     *   public List&lt;EssentialFilter&gt; httpFilters() {
     *       List&lt;EssentialFilter&gt; filters = HttpFiltersComponents.super.httpFilters();
     *       filters.add(loggingFilter);
     *       return filters;
     *   }
     *
     *   // other required methods
     * }
     * </pre>
     *
     * If you want to filter elements out of the list, you can do the following:
     *
     * <pre>
     * class MyComponents extends BuiltInComponentsFromContext implements HttpFiltersComponents {
     *
     *   public MyComponents(ApplicationLoader.Context context) {
     *       super(context);
     *   }
     *
     *   public List&lt;EssentialFilter&gt; httpFilters() {
     *     return httpFilters().stream()
     *          // accept only filters that are not CSRFFilter
     *          .filter(f -&gt; !f.getClass().equals(CSRFFilter.class))
     *          .collect(Collectors.toList());
     *   }
     *
     *   // other required methods
     * }
     * </pre>
     *
     * @return an array with the http filters.
     * @see EssentialFilter
     */
    List<EssentialFilter> httpFilters();

    JavaHandlerComponents javaHandlerComponents();

    HttpRequestHandler httpRequestHandler();
}
