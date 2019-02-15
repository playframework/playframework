/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
      println("Running custom Coordinated Shutdown task.")
      val f = new java.io.File("target/proofs", actorSystem.name + ".txt")
      f.getParentFile.mkdirs
      println(s"Created folder ${f.getParentFile.getAbsolutePath}")
      f.createNewFile()
      println(s"Created file ${f.getAbsolutePath}")
      println("Running custom Coordinated Shutdown task.")
      Future.successful(Done)
  }

  def index = Action {
    Ok("original")
  }
}
