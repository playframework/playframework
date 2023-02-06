/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package scalaguide.akka.components

//#cluster-compile-time-injection
import play.api._
import play.api.cluster.sharding.typed.ClusterShardingComponents
import play.api.routing.Router
import play.api.ApplicationLoader.Context

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
