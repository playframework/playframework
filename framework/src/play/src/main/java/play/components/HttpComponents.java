/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.components;

import play.http.ActionCreator;
import play.http.HttpRequestHandler;
import play.mvc.EssentialFilter;

public interface HttpComponents extends HttpConfigurationComponents {

    ActionCreator actionCreator();

    /**
     * List of filters, typically provided by mixing in play.filters.HttpFiltersComponents
     * or play.api.NoHttpFiltersComponents.
     *
     * In most cases you will want to mixin HttpFiltersComponents and append your own filters:
     *
     * <pre>
     * public class MyComponents extends BuiltInComponentsFromContext implements play.filters.components.HttpFiltersComponents {
     *
     *   public MyComponents(ApplicationLoader.Context context) {
     *       super(context);
     *   }
     *
     *   public EssentialFilter[] httpFilters() {
     *       LoggingFilter loggingFilter = new LoggingFilter();
     *       List&lt;EssentialFilter&gt; filters = Arrays.asList(httpFilters());
     *       filters.add(loggingFilter);
     *       return filters.toArray();
     *   }
     *
     *   // other required methods
     * }
     * </pre>
     *
     * If you want to filter elements out of the list, you can do the following:
     *
     * <pre>
     * class MyComponents extends BuiltInComponentsFromContext implements play.filters.HttpFiltersComponents {
     *
     *   public MyComponents(ApplicationLoader.Context context) {
     *       super(context);
     *   }
     *
     *   public EssentialFilter[] httpFilters() {
     *     return Arrays
     *          .stream(httpFilters())
     *          // accept only filters that are not CSRFFilter
     *          .filter(f -&gt; !f.getClass().equals(CSRFFilter.class))
     *          .toArray();
     *   }
     *
     *   // other required methods
     * }
     * </pre>
     *
     * @return an array with the http filters.
     * @see EssentialFilter
     */
    EssentialFilter[] httpFilters();

    HttpRequestHandler httpRequestHandler();
}
