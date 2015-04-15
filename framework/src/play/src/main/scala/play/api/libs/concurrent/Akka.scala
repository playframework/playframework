/*
 * Copyright (C) 2009-2015 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.libs.concurrent

import com.typesafe.config.Config
import java.util.concurrent.TimeoutException
import javax.inject.{ Provider, Inject, Singleton }
import play.api._
import play.api.inject.{ ApplicationLifecycle, Module }
import play.core.ClosableLazy
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
 * Provider for the actor system
 */
@Singleton
class ActorSystemProvider @Inject() (environment: Environment, configuration: Configuration, applicationLifecycle: ApplicationLifecycle) extends Provider[ActorSystem] {

  private val logger = Logger(classOf[ActorSystemProvider])

  lazy val get: ActorSystem = {
    val (system, stopHook) = ActorSystemProvider.start(environment.classLoader, configuration)
    applicationLifecycle.addStopHook(stopHook)
    system
  }

}

object ActorSystemProvider {

  type StopHook = () => Future[Unit]

  private val logger = Logger(classOf[ActorSystemProvider])

  /**
   * Start an ActorSystem, using the given configuration and ClassLoader.
   * @return The ActorSystem and a function that can be used to stop it.
   */
  def start(classLoader: ClassLoader, configuration: Configuration): (ActorSystem, StopHook) = {
    val config = PlayConfig(configuration)

    val akkaConfig: Config = {
      val akkaConfigRoot = config.get[String]("play.akka.config")
      // Need to fallback to root config so we can lookup dispatchers defined outside the main namespace
      config.get[Config](akkaConfigRoot).withFallback(config.underlying)
    }

    val name = config.get[String]("play.akka.actor-system")
    val system = ActorSystem(name, akkaConfig, classLoader)
    logger.info(s"Starting application default Akka system: $name")

    val stopHook = { () =>
      logger.info(s"Shutdown application default Akka system: $name")
      system.shutdown()

      config.get[Duration]("play.akka.shutdown-timeout") match {
        case timeout: FiniteDuration =>
          try {
            system.awaitTermination(timeout)
          } catch {
            case te: TimeoutException =>
              // oh well.  We tried to be nice.
              logger.info(s"Could not shutdown the Akka system in $timeout milliseconds.  Giving up.")
          }
        case _ =>
          // wait until it is shutdown
          system.awaitTermination()
      }

      Future.successful(())
    }

    (system, stopHook)
  }

  /**
   * A lazy wrapper around `start`. Useful when the `ActorSystem` may
   * not be needed.
   */
  def lazyStart(classLoader: => ClassLoader, configuration: => Configuration): ClosableLazy[ActorSystem, Future[Unit]] = {
    new ClosableLazy[ActorSystem, Future[Unit]] {
      protected def create() = start(classLoader, configuration)
      protected def closeNotNeeded = Future.successful(())
    }
  }

}
