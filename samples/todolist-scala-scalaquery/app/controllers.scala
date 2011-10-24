package controllers

import java.sql.Date

import play.api.mvc._

import play.api.data._
import play.api.data.Form._
import play.api.data.validation.Constraints._

import models._

object Authentication extends Controller {
    
    val loginForm = Form(
        of(
            "login" -> (text verifying required),
            "password" -> text
        ) verifying("Unknown user or bad password", lp => lp._1 == lp._2)
    )
    
    def login = Action { implicit request =>
        Unauthorized(views.html.login(loginForm))
    }
    
    def logout = Action {
        Redirect(routes.Authentication.login).withNewSession
    }
    
    def authenticate = Action { implicit request =>
        loginForm.bindFromRequest().fold(
            errors => BadRequest(views.html.login(errors)),
            {case (user,_) => Redirect(routes.Tasks.list).withSession(session + (Security.USERNAME -> user))}
        )
    }
    
}

object Tasks extends Controller with Security.AllAuthenticated {
    
    val taskForm = Form(
        of(
            "name" -> text(minLength = 3),
            "dueDate" -> sqlDate,
            "done" -> boolean
        )
    )
    
    override def onUnauthorized(request:RequestHeader) = Redirect(routes.Authentication.login)    
    
    def list = Action { implicit request =>
        Ok(views.html.list(Task.findAll))
    }
    
    def form(id:Option[Long]) = Action { implicit request =>
       Ok(views.html.form(id, id.map(id => taskForm.fill(Task.findById(id))).getOrElse(taskForm)))
    }
    
    def save(id:Option[Long]) = Action { implicit request =>
        taskForm.bindFromRequest().fold(
            f => {
                BadRequest(views.html.form(id, f))
            },
            v => {
                id.map(Task.update(_, v)).getOrElse(Task.insert(v))
                Redirect(routes.Tasks.list)
            }
        )
    }
    
    def delete(id:Long) = Action {
        Task.delete(id)
        Redirect(routes.Tasks.list)
    }
    
}