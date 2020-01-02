/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.sharding.typed

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.annotation.InternalApi

@InternalApi
final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Scala) */
@Singleton
@InternalApi
class ClusterShardingProvider @Inject() (val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding(actorSystem.toTyped)
}
