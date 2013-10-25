/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.utils

import scala.collection.immutable.ListMap
import scala.collection.mutable

object OrderPreserving {

  def groupBy[K,V](seq:Seq[(K,V)])(f: ((K,V)) => K): ListMap[K, Seq[V]] = {
    // This mutable map will not retain insertion order for the seq, but it is fast for retrieval. The value is
    // a builder for the desired Seq[String] in the final result.
    val m = mutable.Map.empty[K, mutable.Builder[V, Seq[V]]]

    // Run through the seq and create builders for each unique key, effectively doing the grouping
    for ( (key,value) <- seq) m.getOrElseUpdate(key, mutable.Seq.newBuilder[V]) += value

    // Create a builder for the resulting ListMap. Note that this one is immutable and will retain insertion order
    val b = ListMap.newBuilder[K, Seq[V]]

    // Note that we are NOT going through m (didn't retain order) but we are iterating over the original seq
    // just to get the keys so we can look up the values in m with them. This is how order is maintained.
    for ((k, v) <- seq.iterator)  b += k -> m.getOrElse(k,mutable.Seq.newBuilder[V]).result

    // Get the builder to produce the final result
    b.result
  }

/** A Specialized, order preserving groupBy method.
* This is highly similar to scala.collection.mutable.Seq.groupBy except that it uses a slightly different
* strategy that maintains order.
* @param seq The sequence to be grouped
* @return A ListMap that provides the grouping.
def groupByKeyRetainingInsertionOrder(seq: Seq[(String,String)]): ListMap[String, Seq[String]] = {

// This mutable map will not retain insertion order for the seq, but it is fast for retrieval. The value is
// a builder for the desired Seq[String] in the final result.
val m = mutable.Map.empty[String, mutable.Builder[String, Seq[String]]]

// Run through the seq and create builders for each unique key, effectively doing the grouping
for ( (key, value) <- seq) m.getOrElseUpdate(key, mutable.Seq.newBuilder[String]) += value

// Create a builder for the resulting ListMap. Note that this one is immutable and will retain insertion order
val b = ListMap.newBuilder[String, Seq[String]]

// Note that we are NOT going through m (didn't retain order) but we are iterating over the original seq
// just to get the keys so we can look up the values in m with them. This is how order is maintained.
for ((k, v) <- seq.iterator)  b += k -> m.getOrElse(k,mutable.Seq.newBuilder[String]).result

// Get the builder to produce the final result
b.result
}
*/
}
