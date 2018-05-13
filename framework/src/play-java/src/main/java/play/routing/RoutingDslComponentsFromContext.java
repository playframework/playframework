/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
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
public abstract class RoutingDslComponentsFromContext extends BuiltInComponentsFromContext implements RoutingDslComponents {
    public RoutingDslComponentsFromContext(ApplicationLoader.Context context) {
        super(context);
    }
}
