/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */
package controllers

import akka.Done
import akka.actor.{ ActorSystem, CoordinatedShutdown }
import javax.inject.Inject
import play.api.mvc.{ AbstractController, Action, AnyContent, ControllerComponents }

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ Await, Future }

class HomeController @Inject()(c: ControllerComponents, actorSystem: ActorSystem, cs: CoordinatedShutdown) extends AbstractController(c) {

  // This timestamp is useful in logs to see if a new instance of the Controller is created.
  val startupTs = System.currentTimeMillis()

  // This task generates a file so scripted tests can assert `CoordinatedShutdown` ran.
  cs.addTask(CoordinatedShutdown.PhaseServiceUnbind, "application-cs-proof-of-existence") {
    () =>
      println(s"Producing shutdown proof file for id $startupTs")
      val f = new java.io.File("target/proofs", actorSystem.name + ".txt")
      f.getParentFile.mkdirs
      f.createNewFile()
      Future.successful(Done)
  }

  def index: Action[AnyContent] = Action {
    Ok("original")
  }

  def indexVerbose: Action[AnyContent] = Action {
    Ok(s"verbose - ${actorSystem.whenTerminated.isCompleted} - id: $startupTs")
  }

  case object CustomReason extends CoordinatedShutdown.Reason
  def downing = Action {
    println(s"calling shutdown from controller with id: $startupTs")
    cs.run(CustomReason)
    Ok(s"downing controller with id: $startupTs")
  }
}
