/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

/*
 * A map designed to prevent the "thundering herds" issue.
 *
 * This could be factored out into its own thing, improved and made available more widely. We could also
 * use spray-cache once it has been re-worked into the Akka code base.
 *
 * The essential mechanics of the cache are that all asset requests are remembered, unless their lookup fails or if
 * the asset doesn't exist, in which case we don't remember them in order to avoid an exploit where we would otherwise
 * run out of memory.
 *
 * The population function is executed using the passed-in execution context
 * which may mean that it is on a separate thread thus permitting long running operations to occur. Other threads
 * requiring the same resource will be given the future of the result immediately.
 *
 * There are no explicit bounds on the cache as it isn't considered likely that the number of distinct asset requests would
 * result in an overflow of memory. Bounds are implied given the number of distinct assets that are available to be
 * served by the project.
 *
 * Instead of a SelfPopulatingMap, a better strategy would be to furnish the assets controller with all of the asset
 * information on startup. This shouldn't be that difficult as sbt-web has that information available. Such an
 * approach would result in an immutable map being used which in theory should be faster.
 */
private class SelfPopulatingMap[K, V] {
  import scala.collection.concurrent.TrieMap
  import scala.concurrent.{ ExecutionContext, Future, Promise }
  import scala.util.{ Failure, Success }

  private val store = TrieMap[K, Future[Option[V]]]()

  def putIfAbsent(k: K)(pf: K => Option[V])(implicit ec: ExecutionContext): Future[Option[V]] = {
    lazy val p = Promise[Option[V]]()
    store.putIfAbsent(k, p.future) match {
      case Some(f) => f
      case None =>
        val f = Future(pf(k))(ec.prepare())
        f.onComplete {
          case Failure(_) | Success(None) => store.remove(k)
          case _ => // Do nothing, the asset was successfully found and is now cached
        }
        p.completeWith(f)
        p.future
    }
  }
}