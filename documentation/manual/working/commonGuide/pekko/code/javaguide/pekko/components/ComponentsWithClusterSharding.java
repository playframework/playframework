/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package javaguide.pekko.components;

// #cluster-compile-time-injection
import play.ApplicationLoader;
import play.BuiltInComponentsFromContext;
import play.cluster.sharding.typed.ClusterShardingComponents;
import play.controllers.AssetsComponents;
import play.filters.components.HttpFiltersComponents;
import play.routing.Router;

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
