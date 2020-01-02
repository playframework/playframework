/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.sharding.typed

import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.actor.typed.scaladsl.adapter._
import akka.annotation.ApiMayChange

@ApiMayChange
trait ClusterShardingComponents {
  def actorSystem: ActorSystem
  lazy val clusterSharding: ClusterSharding = new ClusterShardingProvider(actorSystem).get
}
