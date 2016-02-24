/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.iteratee

import org.specs2.mutable._

object TraversableIterateesSpec extends Specification
    with IterateeSpecification with ExecutionSpecification {

  "Traversable.splitOnceAt" should {

    "yield input while predicate is satisfied" in {
      mustExecute(1) { splitEC =>
        val e = Traversable.splitOnceAt[String, Char] { c => c != 'e' }(
          implicitly[String => scala.collection.TraversableLike[Char, String]],
          splitEC)
        mustTransformTo("hello", "there")("h")(e)
      }
    }

  }
}
