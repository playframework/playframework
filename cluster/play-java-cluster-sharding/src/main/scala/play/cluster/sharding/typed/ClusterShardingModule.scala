/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed

import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

import org.apache.pekko.actor.typed.javadsl.Adapter
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.annotation.InternalApi
import org.apache.pekko.cluster.sharding.typed.javadsl.ClusterSharding
import play.api.inject._

@InternalApi
final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Java) */
@Singleton
@InternalApi
class ClusterShardingProvider @Inject() (val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding.get(Adapter.toTyped(actorSystem))
}
