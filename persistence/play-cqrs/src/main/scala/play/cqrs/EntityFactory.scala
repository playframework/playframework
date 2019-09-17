/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.cqrs

import akka.Done
import akka.actor.ActorSystem
import akka.actor.typed.ActorRef
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.sharding.typed.scaladsl._
import akka.persistence.journal.Tagged
import akka.persistence.typed.ExpectingReply
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl._
import scala.reflect.ClassTag
import akka.annotation.ApiMayChange
import akka.cluster.sharding.typed.ShardingEnvelope
import akka.cluster.sharding.typed.ClusterShardingSettings

@ApiMayChange
class EntityFactory[Command: ClassTag, Event, State](
    name: String,
    behaviorFunc: EntityContext => EventSourcedBehavior[Command, Event, State],
    tagger: Tagger[Event],
    actorSystem: ActorSystem
) {

  // Play has an injectable (untyped) ActorySystem. We can just use it.
  private val typedActorSystem = actorSystem.toTyped
  private val clusterSharding  = ClusterSharding(typedActorSystem)

  val typeKey: EntityTypeKey[Command] = EntityTypeKey[Command](name)

  final def entityRefFor(entityId: String): EntityRef[Command] = {
    // TODO: this need to be removed once https://github.com/akka/akka/pull/27725 is merged
    // this will generate persistence Id compatible with Lagom's Ids, eg: 'ModelName|entityId'
    val persistenceId = typeKey.persistenceIdFrom(entityId)
    clusterSharding.entityRefFor(typeKey, persistenceId.id)
  }

  def buildEntity(): Entity[Command, ShardingEnvelope[Command]] = {
    Entity(
      typeKey,
      ctx =>
        behaviorFunc(ctx)
          .withTagger(tagger.tagFunction(ctx.entityId))
    )
  }

  clusterSharding.init(buildEntity())
}
