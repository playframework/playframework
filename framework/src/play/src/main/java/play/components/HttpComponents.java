/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.components;

import play.http.ActionCreator;
import play.http.HttpRequestHandler;
import play.mvc.EssentialFilter;
import play.mvc.FileMimeTypes;

public interface HttpComponents extends HttpConfigurationComponents {

    ActionCreator actionCreator();

    /**
     * List of filters, typically provided by mixing in play.filters.HttpFiltersComponents
     * or play.api.NoHttpFiltersComponents.
     *
     * In most cases you will want to mixin HttpFiltersComponents and append your own filters:
     *
     * <pre>
     * class MyComponents(context: ApplicationLoader.Context)
     *   extends BuiltInComponentsFromContext(context)
     *   with play.filters.HttpFiltersComponents {
     *
     *   lazy val loggingFilter = new LoggingFilter()
     *   override def httpFilters = {
     *     super.httpFilters :+ loggingFilter
     *   }
     * }
     * </pre>
     *
     * If you want to filter elements out of the list, you can do the following:
     *
     * <pre>
     * class MyComponents(context: ApplicationLoader.Context)
     *   extends BuiltInComponentsFromContext(context)
     *   with play.filters.HttpFiltersComponents {
     *   override def httpFilters = {
     *     super.httpFilters.filterNot(_.getClass == classOf[CSRFFilter])
     *   }
     * }
     * </pre>
     */
    EssentialFilter[] httpFilters();

    HttpRequestHandler httpRequestHandler();
}
