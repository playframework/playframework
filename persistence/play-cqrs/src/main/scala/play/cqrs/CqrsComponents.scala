/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cqrs
import akka.actor.ActorSystem
import akka.cluster.sharding.typed.scaladsl.ClusterSharding
import akka.cluster.sharding.typed.scaladsl.Entity
import akka.cluster.sharding.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import akka.persistence.typed.scaladsl.ReplyEffect

import scala.reflect.ClassTag

import akka.annotation.ApiMayChange

@ApiMayChange
trait CqrsComponents {

  def clusterSharding: ClusterSharding

  final def createEntityFactory[Command: ClassTag, Event, State](
      name: String,
      behaviorFunc: EntityContext => EventSourcedBehavior[Command, Event, State],
      tagger: Tagger[Event]
  ): EntityFactory[Command, Event, State] =
    new EntityFactory(name, behaviorFunc, tagger, clusterSharding)

}
