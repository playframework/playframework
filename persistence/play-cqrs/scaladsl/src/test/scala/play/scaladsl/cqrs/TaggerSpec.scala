/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.scaladsl.cqrs

import org.specs2.mutable._
import akka.persistence.typed.PersistenceId

class TaggerSpec extends Specification {

  "A Tagger" should {

    val persistenceId = "dummy-test-id"

    "shard a tag when configured for sharding" in {
      val tagger      = Tagger[TestEvent].addTagGroup("TagA", 10)
      val shardedTags = tagger.tagFunction(persistenceId)(TestEventA)
      shardedTags must beEqualTo(Set("TagA9"))
    }

    "return all sharded tags for specific tag groups" in {

      val tagger =
        Tagger[TestEvent]
          .addTagGroup("TagA", 3)
          .addTagGroup("TagB", 4)

      val tagsForA = tagger.allShardedTags("TagA")
      tagsForA must haveSize(3)
      tagsForA must_== Set("TagA0", "TagA1", "TagA2")

      val tagsForB = tagger.allShardedTags("TagB")
      tagsForB must haveSize(4)
      tagsForB must_== Set("TagB0", "TagB1", "TagB2", "TagB3")

    }

    "NOT shard tag when no shard number is provided" in {
      val tagger         = Tagger[TestEvent].addTagGroup("TagA")
      val allShardedTags = tagger.tagFunction(persistenceId)(TestEventA)
      allShardedTags must beEqualTo(Set("TagA"))
    }

    "apply tags according to each defined tag group" in {
      val taggers =
        Tagger[TestEventA]
          .addTagGroup("TagA", 10)
          .addTagGroup("TagB", 6)
          .addTagGroup("TagC")

      val tags = taggers.tagFunction(persistenceId)(TestEventA)
      tags must haveSize(3)
      tags must_== Set("TagA9", "TagB3", "TagC")
    }

    "honour each tag group predicate" in {

      val predicateForA: TestEvent => Boolean = {
        case TestEventA => true
        case _          => false
      }

      val predicateForB: TestEvent => Boolean = {
        case TestEventB => true
        case _          => false
      }

      val taggers =
        Tagger[TestEvent]
          .addTagGroup("TagA", 10, predicateForA) // only tag TestEventA
          .addTagGroup("TagB", 6, predicateForB)  // only tag TestEventB

      val tagsForA = taggers.tagFunction(persistenceId)(TestEventA)
      tagsForA must haveSize(1)
      tagsForA must_== Set("TagA9")

      val tagsForB = taggers.tagFunction(persistenceId)(TestEventB)
      tagsForB must haveSize(1)
      tagsForB must_== Set("TagB3")
    }

    "fail when passed a negative shard number" in {
      Tagger[TestEvent].addTagGroup("TagA", -10) must throwA[IllegalArgumentException]
    }

    "fail when passed an empty string" in {
      Tagger[TestEvent].addTagGroup("") must throwA[IllegalArgumentException]
      Tagger[TestEvent].addTagGroup("  ") must throwA[IllegalArgumentException]
    }

  }

  sealed trait TestEvent
  sealed trait TestEventA extends TestEvent
  case object TestEventA  extends TestEventA
  sealed trait TestEventB extends TestEvent
  case object TestEventB  extends TestEventB

}
