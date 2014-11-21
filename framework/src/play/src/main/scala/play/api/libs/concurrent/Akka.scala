/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import java.util.concurrent.{ TimeUnit, TimeoutException }
import javax.inject.{ Provider, Inject, Singleton }
import play.api._
import play.api.inject.{ ApplicationLifecycle, Module }
import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Helper to access the application defined Akka Actor system.
 */
object Akka {

  private val actorSystemCache = Application.instanceCache[ActorSystem]

  /**
   * Retrieve the application Akka Actor system.
   *
   * Example:
   * {{{
   * val newActor = Akka.system.actorOf[Props[MyActor]]
   * }}}
   */
  def system(implicit app: Application): ActorSystem = actorSystemCache(app)

}

/**
 * Components for configuring Akka.
 */
trait AkkaComponents {

  def environment: Environment
  def configuration: Configuration
  def applicationLifecycle: ApplicationLifecycle

  lazy val actorSystem: ActorSystem = new ActorSystemProvider(environment, configuration, applicationLifecycle).get
}

/**
 * The Akka module.
 */
class AkkaModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = Seq(
    bind[ActorSystem].toProvider[ActorSystemProvider]
  )
}

/**
 * Provider for the actor system
 */
@Singleton
class ActorSystemProvider @Inject() (environment: Environment, configuration: Configuration, applicationLifecycle: ApplicationLifecycle) extends Provider[ActorSystem] {

  private val logger = Logger(classOf[ActorSystemProvider])

  lazy val get: ActorSystem = {
    val config = configuration.underlying
    val name = configuration.getString("play.modules.akka.actor-system").getOrElse("application")
    val system = ActorSystem(name, config, environment.classLoader)
    logger.info(s"Starting application default Akka system: $name")

    applicationLifecycle.addStopHook { () =>
      logger.info(s"Shutdown application default Akka system: $name")
      system.shutdown()

      configuration.getMilliseconds("play.akka.shutdown-timeout") match {
        case Some(timeout) =>
          try {
            system.awaitTermination(Duration(timeout, TimeUnit.MILLISECONDS))
          } catch {
            case te: TimeoutException =>
              // oh well.  We tried to be nice.
              logger.info(s"Could not shutdown the Akka system in $timeout milliseconds.  Giving up.")
          }
        case None =>
          // wait until it is shutdown
          system.awaitTermination()
      }

      Future.successful(())
    }

    system
  }

}
