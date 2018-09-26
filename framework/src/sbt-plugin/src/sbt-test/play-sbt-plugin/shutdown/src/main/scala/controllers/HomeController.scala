/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import play.api.mvc._
import javax.inject.Inject
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import java.util.concurrent.atomic.AtomicBoolean

import akka.Done

import scala.concurrent.Future

class HomeController @Inject()(c: ControllerComponents, actorSystem: ActorSystem, cs: CoordinatedShutdown) extends AbstractController(c) {

  // This task generates a file so scripted tests can assert `CoordinatedShutdown` ran.
  cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "application-cs-proof-of-existence") {
    () =>
      val f = new java.io.File("target/proofs", actorSystem.name + ".txt")
      f.getParentFile.mkdirs
      f.createNewFile()
      Future.successful(Done)
  }

  def index = Action {
    Ok("original")
  }
}
