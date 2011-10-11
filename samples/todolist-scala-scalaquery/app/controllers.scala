package controllers

import java.sql.Date

import play.api.mvc._

import org.scalaquery.ql.extended.H2Driver.Implicit._ 

import play.api.data._
import play.api.data.Form._
import play.api.data.validation.Constraints._

import ScalaQuery._

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
        loginForm.bind().fold(
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
        withSession { implicit db =>
            Ok(views.html.list { (for(t <- models.Tasks) yield t.id ~ t.name).list })
        }
    }
    
    def create = Action { implicit request =>
        Ok(views.html.form(None, taskForm))
    }
    
    def save = Action { implicit request =>
        taskForm.bind().fold(
            f => BadRequest(views.html.form(None, f)),
            t => {
                withSession { implicit db =>
                    models.Tasks.noID.insert(t)
                }
                Redirect(routes.Tasks.list)
            }
        )
    }
    
    def edit(id:Long) = Action { implicit request =>
        Ok(views.html.form(Some(id), taskForm.fill { 
            withSession { implicit db =>
                (for(t <- models.Tasks if t.id === id) yield t.name ~ t.dueDate ~ t.done).first
            }
        }))
    }

    
    def update(id:Long) = Action { implicit request =>
        taskForm.bind().fold(
            f => BadRequest(views.html.form(Some(id), f)),
            t => {
                withSession { implicit db =>
                    (for(t <- models.Tasks if t.id === id) yield t.name ~ t.dueDate ~ t.done).update(t)
                }
                Redirect(routes.Tasks.list)
            }
        )
    }
    
    def delete(id:Long) = Action {
        withSession { implicit db =>
            models.Tasks.where(_.id ===  id).delete
            Redirect(routes.Tasks.list)
        }
    }
    
}