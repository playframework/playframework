/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.components

//#cluster-compile-time-injection
import play.api._
import play.api.ApplicationLoader.Context
import play.api.routing.Router
import play.api.cluster.sharding.typed.ClusterShardingComponents

class MyApplicationLoader extends ApplicationLoader {
  def load(context: Context) = {
    new ComponentsWithClusterSharding(context).application
  }
}

class ComponentsWithClusterSharding(context: Context)
    extends BuiltInComponentsFromContext(context)
    with play.filters.HttpFiltersComponents
    with ClusterShardingComponents {

  lazy val router = Router.empty
}
//#cluster-compile-time-injection
