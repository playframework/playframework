/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.internal.javadsl.cluster.sharding.typed

import akka.cluster.sharding.typed.javadsl.ClusterSharding
import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.typed.javadsl.Adapter
import akka.actor.ActorSystem

final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Java) */
@Singleton
class ClusterShardingProvider @Inject()(val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  private val sharding = ClusterSharding.get(Adapter.toTyped(actorSystem))
  def get(): ClusterSharding = sharding
}
