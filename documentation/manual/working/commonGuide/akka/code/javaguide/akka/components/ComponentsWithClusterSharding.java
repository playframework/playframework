/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.akka.components;

// #cluster-compile-time-injection
import play.Application;
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.controllers.AssetsComponents;
import play.routing.Router;
import play.cluster.sharding.typed.ClusterShardingComponents;
import play.filters.components.HttpFiltersComponents;

public class ComponentsWithClusterSharding extends BuiltInComponentsFromContext
    implements ClusterShardingComponents, AssetsComponents, HttpFiltersComponents {

  public ComponentsWithClusterSharding(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
// #cluster-compile-time-injection
