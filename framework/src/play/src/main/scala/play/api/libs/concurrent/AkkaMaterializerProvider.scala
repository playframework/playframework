/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import javax.inject.{ Inject, Provider, Singleton }

import akka.actor.ActorSystem
import akka.stream.Supervision.Decider
import akka.stream.{ ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision }
import play.api.inject.{ Binding, BindingKey }
import play.api.{ Configuration, Environment, Logger }
import play.utils.Reflect

private[play] object AkkaMaterializerProvider {

  def bindingsFromConfiguration(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Reflect.configuredClass[Provider[Materializer], Provider[Materializer], DefaultAkkaMaterializerProvider](
      environment,
      configuration, "play.akka.materializer.provider", "ActionCreator").fold(Seq[Binding[_]]()) { either =>
      val impl = either.fold(identity, identity)
      Seq(BindingKey(classOf[Materializer]).toProvider(impl))
    }
  }

}

/**
 * Provider for the default flow materializer
 */
@Singleton
class DefaultAkkaMaterializerProvider @Inject() (actorSystem: ActorSystem) extends Provider[Materializer] {

  private val logger = Logger(this.getClass)

  protected def decider: Decider = { e =>
    logger.error("Unhandled exception in stream", e)
    Supervision.Stop
  }

  override lazy val get: Materializer = {
    ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))(actorSystem)
  }

}