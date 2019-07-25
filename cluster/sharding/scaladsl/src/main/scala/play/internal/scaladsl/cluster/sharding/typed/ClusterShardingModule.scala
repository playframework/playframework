/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.internal.scaladsl.cluster.sharding.typed

import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._

final class ClusterShardingModule extends SimpleModule(bind[ClusterSharding].toProvider[ClusterShardingProvider])

/** Provider for the Akka Typed ClusterSharding (Scala) */
@Singleton
class ClusterShardingProvider @Inject()(val actorSystem: ActorSystem) extends Provider[ClusterSharding] {
  private val sharding =  ClusterSharding(actorSystem.toTyped)
  def get(): ClusterSharding = sharding
}
