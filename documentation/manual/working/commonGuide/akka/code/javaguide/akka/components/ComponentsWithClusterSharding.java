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
import play.javadsl.cluster.sharding.typed.ClusterShardingComponents;

public class ComponentsWithClusterSharding extends BuiltInComponentsFromContext
    implements ClusterShardingComponents, AssetsComponents {

  public ComponentsWithClusterSharding(ApplicationLoader.Context context) {
    super(context);
  }

  @Override
  public Router router() {
    return Router.empty();
  }
}
// #cluster-compile-time-injection
