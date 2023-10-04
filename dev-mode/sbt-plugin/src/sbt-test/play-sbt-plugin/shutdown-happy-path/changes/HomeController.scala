/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package controllers

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.Future

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.Done
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import play.api.libs.concurrent.Futures
import play.api.mvc._

class HomeController @Inject() (
    val controllerComponents: ControllerComponents,
    actorSystem: ActorSystem,
    cs: CoordinatedShutdown,
    futures: Futures
)(implicit executionContext: ExecutionContext)
    extends BaseController {

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
    futures.delay(2.seconds).map(_ => Ok("DONE"))
  }
}

// ATTENTION: diff to force a reload, but virtually then same as the other file.
