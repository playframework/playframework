package controllers

import play.api._
import play.api.mvc._
import play.api.data._

import models._
import views._

object Application extends Controller {

  // -- Authentication

  val loginForm = Form(
    of(
      "email" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => User.authenticate(email, password).isDefined
    })
  )

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(loginForm))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.login(formWithErrors)),
      user => Redirect(routes.Projects.index).withSession("email" -> user._1)
    )
  }

  /**
   * Logout and clean the session.
   */
  def logout = Action {
    Redirect(routes.Application.login).withNewSession.flashing(
      "success" -> "You've been logged out"
    )
  }

  // -- Javascript routing

  def javascriptRoutes = Action {
    import routes.javascript._
    Ok(
      Routes.javascriptRouter("jsRoutes")(
        Projects.add, Projects.delete, Projects.rename,
        Projects.addGroup, Projects.deleteGroup, Projects.renameGroup,
        Projects.addUser, Projects.removeUser, Tasks.addFolder, 
        Tasks.renameFolder, Tasks.deleteFolder, Tasks.index,
        Tasks.add, Tasks.update, Tasks.delete
      )
    ).as("text/javascript") 
  }

}

/**
 * Provide security features
 */
trait Secured extends Security.AllAuthenticated {

  /**
   * Retrieve the connected user email.
   */
  override def username(request: RequestHeader) = request.session.get("email")

  /**
   * Redirect to login if the use in not authorized.
   */
  override def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)

  /**
   * Check if the connected user is a member of this project.
   */
  def IsMemberOf(project: Long)(f: => Result)(implicit request: RequestHeader) = {
    request.username.filter(Project.isMember(project, _)).map(_ => f).getOrElse(Results.Forbidden)
  }

  /**
   * Check if the connected user is a owner of this task.
   */
  def IsOwnerOf(task: Long)(f: => Result)(implicit request: RequestHeader) = {
    request.username.filter(Task.isOwner(task, _)).map(_ => f).getOrElse(Results.Forbidden)
  }

}

