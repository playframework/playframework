/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.routing;

import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;

/**
 * RoutingDsl components from the built in components.
 *
 * @see play.BuiltInComponentsFromContext
 * @see play.routing.RoutingDslComponents
 */
public abstract class RoutingDslComponentsFromContext extends BuiltInComponentsFromContext
    implements RoutingDslComponents {
  public RoutingDslComponentsFromContext(ApplicationLoader.Context context) {
    super(context);
  }
}
