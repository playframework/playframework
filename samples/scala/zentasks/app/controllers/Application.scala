package controllers

import play.api._
import play.api.mvc._
import play.api.data.mapping._
import play.api.libs.functional.syntax._

import models._
import views._

object Application extends Controller {

  // -- Authentication

  val isAuthenticated = Rules.validateWith[(String, String)]("Invalid email or password"){
    case (email, password) => User.authenticate(email, password).isDefined
  }

  implicit val loginValidation = From[UrlFormEncoded] { __ =>
    import Rules._
    ((__ \ "email").read(notEmpty) ~
     (__ \ "password").read(notEmpty)).tupled
  }.compose(isAuthenticated)

  /**
   * Login page.
   */
  def login = Action { implicit request =>
    Ok(html.login(Form()))
  }

  /**
   * Handle login form submission.
   */
  def authenticate = Action(parse.urlFormEncoded) { implicit request =>
    val r = loginValidation.validate(request.body)
    r match {
      case Failure(_) => BadRequest(html.login(Form(request.body, r)))
      case Success(user) => Redirect(routes.Projects.index).withSession("email" -> user._1)
    }
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

  def javascriptRoutes = Action { implicit request =>
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
trait Secured {

  /**
   * Retrieve the connected user email.
   */
  private def username(request: RequestHeader) = request.session.get("email")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)

  // --

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }

  /**
   * Check if the connected user is a member of this project.
   */
  def IsMemberOf(project: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Project.isMember(project, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }

  /**
   * Check if the connected user is a owner of this task.
   */
  def IsOwnerOf(task: Long)(f: => String => Request[AnyContent] => Result) = IsAuthenticated { user => request =>
    if(Task.isOwner(task, user)) {
      f(user)(request)
    } else {
      Results.Forbidden
    }
  }

}

