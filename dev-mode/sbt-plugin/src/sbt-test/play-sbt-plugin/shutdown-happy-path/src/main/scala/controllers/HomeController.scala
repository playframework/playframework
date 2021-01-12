/*
 * Copyright (C) Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import play.api.mvc._
import javax.inject.Inject
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import java.util.concurrent.atomic.AtomicBoolean
import play.api.libs.concurrent.Futures

import akka.Done

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HomeController @Inject()(
  val controllerComponents: ControllerComponents,
  actorSystem: ActorSystem,
  cs: CoordinatedShutdown,
  futures: Futures
)(implicit executionContext: ExecutionContext) extends BaseController {

  private val logger = LoggerFactory.getLogger(classOf[HomeController])

  // This task generates a file so scripted tests can assert `CoordinatedShutdown` ran.
  cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "application-cs-proof-of-existence") { () =>
    logger.info("Running custom Coordinated Shutdown task.")
    val f = new java.io.File("target/proofs", actorSystem.name + ".txt")
    f.getParentFile.mkdirs
    logger.info(s"Created folder ${f.getParentFile.getAbsolutePath}")
    f.createNewFile()
    logger.info(s"Created file ${f.getAbsolutePath}")
    logger.info("Running custom Coordinated Shutdown task.")
    Future.successful(Done)
  }

  def index = Action {
    Ok("original")
  }

  def slow = Action.async {
    logger.info(s"/slow got request, delaying response for 2 seconds")
    futures.delay(2.seconds).map(_ => Ok("DONE"))
  }
}
