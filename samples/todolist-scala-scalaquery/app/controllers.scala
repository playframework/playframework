package controllers

import java.sql.Date

import play.api.mvc._

import org.scalaquery.ql.extended.H2Driver.Implicit._ 

import play.api.data._
import play.api.data.Form._

object Tasks extends Controller with ScalaQuery {
    
    val taskForm = Form(
        of(
            "name" -> text(minLength = 3),
            "dueDate" -> sqlDate,
            "done" -> boolean
        )
    )
    
    // --
    
    def list = DBAction { implicit ctx =>
        Ok(views.html.list { (for(t <- models.Tasks) yield t.id ~ t.name).list } )
    }
    
    def create = Action {
        Ok(views.html.form(None, taskForm))
    }
    
    def save = DBAction { implicit ctx =>
        taskForm.bind().fold(
            f => BadRequest(views.html.form(None, f)),
            t => {
                models.Tasks.noID.insert(t)
                Redirect(routes.Tasks.list)
            }
        )
    }
    
    def edit(id:Long) = DBAction { implicit ctx =>
        Ok(views.html.form(Some(id), taskForm.fill { 
            (for(t <- models.Tasks if t.id === id) yield t.name ~ t.dueDate ~ t.done).first
        }))
    }
    
    def update(id:Long) = DBAction { implicit ctx =>
        taskForm.bind().fold(
            f => BadRequest(views.html.form(Some(id), f)),
            t => {
                (for(t <- models.Tasks if t.id === id) yield t.name ~ t.dueDate ~ t.done).update(t)
                Redirect(routes.Tasks.list)
            }
        )
    }
    
    def delete(id:Long) = DBAction { implicit ctx =>
        models.Tasks.where(_.id ===  id).delete
        Redirect(routes.Tasks.list)
    }
    
}