/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.concurrent

import java.util.{ function => japi }
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
      configuration, "play.akka.materializer.provider", classOf[DefaultAkkaMaterializerProvider].getName).fold(Seq[Binding[_]]()) { either =>
      val impl = either.fold(identity, identity)
      // Ensures that the Provider is bound Singleton, always and Bounds the Provider to the Materializer
      Seq(BindingKey(impl).toSelf.in(classOf[Singleton]), BindingKey(classOf[Materializer]).toProvider(impl))
    }
  }

}

/**
 * Provider for the default flow materializer
 */
@Singleton
class DefaultAkkaMaterializerProvider @Inject() (actorSystem: ActorSystem) extends Provider[Materializer] {

  private val logger = Logger(this.getClass)

  protected def supervisionDecider: japi.Function[Throwable, Supervision.Directive] = {
    (t: Throwable) =>
      {
        logger.error("Unhandled exception in stream", t)
        Supervision.Stop
      }
  }

  protected def decider: Decider = { t =>
    supervisionDecider.apply(t)
  }

  override lazy val get: Materializer = {
    ActorMaterializer(ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))(actorSystem)
  }

}