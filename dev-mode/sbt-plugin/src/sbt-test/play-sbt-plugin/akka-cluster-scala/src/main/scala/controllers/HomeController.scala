package controllers

import javax.inject.Inject
import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.typed.Cluster
import play.api.mvc._

/**
 * A very small controller that renders a home page.
 */
class HomeController @Inject()(cc: ControllerComponents, 
                               actorSystem: ActorSystem) extends AbstractController(cc) {


  private val typedActorSystem = actorSystem.toTyped
  private val cluster          = Cluster(typedActorSystem)

  def index = Action { implicit request =>
    val status = cluster.selfMember.status
    Ok(s"Cluster is $status")
  }
}
