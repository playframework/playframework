package controllers

import java.sql.Date

import play.api.mvc._

import org.scalaquery.ql.extended.H2Driver.Implicit._ 

import play.api.data._
import play.api.data.Form._
import play.api.data.validation.Constraints._

object Authentication extends Controller {
    
    val loginForm = Form(
        of(
            "login" -> (text verifying required),
            "password" -> text
        ) verifying("Unknown user or bad password", lp => lp._1 == lp._2)
    )
    
    def login = Unauthorized(views.html.login(loginForm))
    
    def logout = Action { implicit ctx =>
        Redirect(routes.Authentication.login).withNewSession
    }
    
    def authenticate = Action { implicit ctx =>
        loginForm.bind().fold(
            errors => BadRequest(views.html.login(errors)),
            {case (user,_) => Redirect(routes.Tasks.list).withSession("user" -> user)}
        )
    }
    
}

object Tasks extends Controller with ScalaQuery with AllSecured {
    
    val taskForm = Form(
        of(
            "name" -> text(minLength = 3),
            "dueDate" -> sqlDate,
            "done" -> boolean
        )
    )
    
    // --
    
    def isAuthentified(request:RequestHeader) = request.session.contains("user")
    override def onUnauthorized(request:RequestHeader) = Redirect(routes.Authentication.login)
    
    // --
    
    def list = Action {
        withSession { implicit db =>
            Ok(views.html.list { (for(t <- models.Tasks) yield t.id ~ t.name).list })
        }
    }
    
    def create = Action {
        Ok(views.html.form(None, taskForm))
    }
    
    def save = Action { implicit ctx =>
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
    
    def edit(id:Long) = Action { 
        Ok(views.html.form(Some(id), taskForm.fill { 
            withSession { implicit db =>
                (for(t <- models.Tasks if t.id === id) yield t.name ~ t.dueDate ~ t.done).first
            }
        }))
    }
    
    def update(id:Long) = Action { implicit ctx =>
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
        }
        Redirect(routes.Tasks.list)
    }
    
}