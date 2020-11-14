/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.cluster.sharding.typed

import akka.cluster.sharding.typed.javadsl.ClusterSharding
import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.typed.javadsl.Adapter
import akka.actor.ActorSystem
import akka.annotation.InternalApi

@InternalApi
final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Java) */
@Singleton
@InternalApi
class ClusterShardingProvider @Inject() (val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding.get(Adapter.toTyped(actorSystem))
}
