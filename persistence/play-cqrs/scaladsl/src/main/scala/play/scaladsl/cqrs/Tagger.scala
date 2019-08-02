/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs

import akka.annotation.ApiMayChange

final class Tagger[Event](tagGroups: List[TagGroup[Event]] = Nil) {

  final def tagFunction(persistenceId: String): Event => Set[String] = { event =>
    {
      val tags = tagGroups.map { taggers =>
        taggers.tagFunction(persistenceId)(event)
      }
      tags.toSet.flatten
    }
  }

  final def allShardedTags(tag: String): Set[String] = {
    tagGroups
      .find(_.originalTag == tag)
      .map { tagGroup =>
        if (tagGroup.numOfShards > 1) {
          val shardedTag =
            for { shardId <- 0 until tagGroup.numOfShards } yield tagGroup.shardTag(shardId)
          shardedTag.toSet
        } else Set(tagGroup.originalTag)
      }
      .getOrElse(Set.empty)
  }

  def addTagGroup(tag: String, numOfShards: Int, predicate: Event => Boolean = _ => true): Tagger[Event] =
    addTagGroup(DefaultTagger[Event](tag, numOfShards, predicate))

  def addTagGroup(tag: String, predicate: Event => Boolean): Tagger[Event] =
    addTagGroup(DefaultTagger[Event](tag, 1, predicate))

  def addTagGroup(tag: String): Tagger[Event] =
    addTagGroup(DefaultTagger[Event](tag, 1, _ => true))

  def addTagGroup(tagGroup: TagGroup[Event]): Tagger[Event] =
    new Tagger(tagGroups :+ tagGroup)
}

@ApiMayChange
object Tagger {
  def apply[Event] = new Tagger[Event]()
}

@ApiMayChange
trait TagGroup[Event] {

  val numOfShards: Int
  val originalTag: String
  val predicate: Event => Boolean

  def shardTag(shardNum: Int): String

  /** Return the tag function that will be used to tag the events */
  def tagFunction(persistenceId: String): Event => Option[String]
}
@ApiMayChange
case class DefaultTagger[Event](originalTag: String, numOfShards: Int, predicate: Event => Boolean)
    extends TagGroup[Event] {

  require(numOfShards >= 1, "Must use a Natural number for `numOfShards`")
  require(originalTag.trim.nonEmpty, "Tag must be a non-empty String")

  final def shardTag(shardNum: Int): String =
    if (numOfShards > 1) originalTag + shardNum
    else originalTag

  final def tagFunction(persistenceId: String): Event => Option[String] =
    evt => {
      if (predicate(evt)) Some(shardTag(Math.abs(persistenceId.hashCode % numOfShards)))
      else None
    }
}
