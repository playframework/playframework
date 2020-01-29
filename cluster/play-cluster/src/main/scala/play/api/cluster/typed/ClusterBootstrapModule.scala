/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.cluster.typed

import play.api.inject._
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import akka.actor.ActorSystem
import akka.annotation.InternalApi
import akka.cluster.typed.Cluster
import akka.cluster.typed.Join
import play.api.Configuration
import play.api.Environment
import play.api.inject.Module
@InternalApi
final class ClusterBootstrapModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    // eagerly bootstrap the cluster
    bind[PlayClusterBootstrap].toSelf.eagerly()
  )
}
