/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.sharding.typed

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.ApiMayChange
import org.apache.pekko.cluster.sharding.typed.scaladsl.ClusterSharding

@ApiMayChange
trait ClusterShardingComponents {
  def actorSystem: ActorSystem
  lazy val clusterSharding: ClusterSharding = new ClusterShardingProvider(actorSystem).get
}
