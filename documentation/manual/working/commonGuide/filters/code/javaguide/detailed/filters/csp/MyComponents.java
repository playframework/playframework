/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.detailed.filters.csp;

import java.util.ArrayList;
import java.util.List;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.filters.components.CSPComponents;
import play.filters.components.HttpFiltersComponents;
import play.mvc.EssentialFilter;
import play.routing.Router;

// #java-csp-components
public class MyComponents extends BuiltInComponentsFromContext
    implements HttpFiltersComponents, CSPComponents {

  public MyComponents(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public List<play.mvc.EssentialFilter> httpFilters() {
    List<EssentialFilter> parentFilters = HttpFiltersComponents.super.httpFilters();
    List<EssentialFilter> newFilters = new ArrayList<>();
    newFilters.add(cspFilter().asJava());
    newFilters.addAll(parentFilters);
    return newFilters;
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
// #java-csp-components
