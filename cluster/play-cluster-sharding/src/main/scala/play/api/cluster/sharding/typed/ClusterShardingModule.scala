/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.sharding.typed

import akka.actor.typed.scaladsl.adapter._
import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import jakarta.inject.Inject
import jakarta.inject.Provider
import jakarta.inject.Singleton
import play.api.inject._

@InternalApi
final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Scala) */
@Singleton
@InternalApi
class ClusterShardingProvider @Inject() (val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  val get: ClusterSharding = ClusterSharding(actorSystem.toTyped)
}
