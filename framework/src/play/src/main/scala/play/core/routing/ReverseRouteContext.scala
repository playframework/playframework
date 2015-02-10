/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.routing

/**
 * The context for a reverse route.
 *
 * This is made available implicitly to PathBindables and QueryStringBindables in the reverse router so that they can
 * query the fixed params that are passed to the action.
 *
 * An empty reverse router context is made available implicitly to the router and JavaScript router.
 *
 * @param fixedParams The fixed params that this route passes to the action.
 */
case class ReverseRouteContext(fixedParams: Map[String, Any])

object ReverseRouteContext {
  implicit val empty = ReverseRouteContext(Map())
}
