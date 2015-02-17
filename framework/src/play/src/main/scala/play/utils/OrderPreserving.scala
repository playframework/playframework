/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import scala.collection.immutable.ListMap
import scala.collection.mutable

object OrderPreserving {

  def groupBy[K, V](seq: Seq[(K, V)])(f: ((K, V)) => K): Map[K, Seq[V]] = {
    // This mutable map will not retain insertion order for the seq, but it is fast for retrieval. The value is
    // a builder for the desired Seq[String] in the final result.
    val m = mutable.Map.empty[K, mutable.Builder[V, Seq[V]]]

    // Run through the seq and create builders for each unique key, effectively doing the grouping
    for ((key, value) <- seq) m.getOrElseUpdate(key, mutable.Seq.newBuilder[V]) += value

    // Create a builder for the resulting ListMap. Note that this one is immutable and will retain insertion order
    val b = ListMap.newBuilder[K, Seq[V]]

    // Note that we are NOT going through m (didn't retain order) but we are iterating over the original seq
    // just to get the keys so we can look up the values in m with them. This is how order is maintained.
    for ((k, v) <- seq.iterator) b += k -> m.getOrElse(k, mutable.Seq.newBuilder[V]).result

    // Get the builder to produce the final result
    b.result
  }
}
