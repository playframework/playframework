/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.typed

import akka.actor.ActorSystem

import play.api.Configuration
import play.api.Environment
import akka.actor.typed.scaladsl.adapter._
import akka.annotation.ApiMayChange

@ApiMayChange
trait ClusterBootstrapComponents {

  def actorSystem: ActorSystem
  def configuration: Configuration
  def environment: Environment

  // eagerly bootstrap the cluster
  private val _ = new PlayClusterBootstrap(actorSystem, configuration, environment)
}
