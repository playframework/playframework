package controllers

import models._
import play.api.mvc._
import java.util.Date
import play.api.data._

object Application extends Controller {

    def login = Action {
        Ok(views.html.auth.login())
    }
    def doLogin = TODO
    def doLogout = TODO
    
    def register = Action {
        Ok(views.html.auth.register())
    }
    def doRegister = TODO

    def recover = Action {
        Ok(views.html.auth.recover())
    }
    def doRecover = TODO

}

object Projects extends Controller {

    def index = Action {
        // Should load oaverdue tasks only
        val tasks = Task.findAll
        // Should load current user
        val user = User.findById(1).get
        Ok(views.html.dashboard( Project.findByUser, tasks, user, User.findAll ))
    }

    def add = Action { implicit request =>
        Form("group" -> text).bind.fold(
            f => BadRequest,
            v => Ok( views.html.projects.item( Project.add(v) ) )
        )
    }

    def get(id: Long) = Action {
        Redirect( routes.Tasks.index( id ) )
    }
    def delete(id: Long) = Action { Ok }
    def rename(id: Long) = Action { implicit request =>
        Form("name" -> text).bind.fold(
            f => BadRequest,
            v => {
                Ok( v )
            }
        )
    }

    // We use lazy groups and folder.
    // But we need those actions to fetch templates.
    def addGroup = Action {
        Ok( views.html.projects.group( "New group" ){ null }  )
    }
    def deleteGroup(id: String) = Action { Ok }
    def renameGroup(id: String) = Action { implicit request =>
        Form("name" -> text).bind.fold(
            f => BadRequest,
            v => {
                Ok( v )
            }
        )
    }
    def addUser(project: Long) = Action { Ok }
    def removeUser(project: Long) = Action { Ok }
}

object Tasks extends Controller {

    def date(str:String):java.util.Date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
    def index(project: Long) = Action {
        val fakeUsers = User.findAll.filter( _.id <= 3 )
        Ok(views.html.tasks.index( Project.findById(project).get, Task.findByProject(project), fakeUsers ))
    }
    def add(project: Long) = Action { implicit request =>
        Form("title" -> text).bind.fold(
            // Other params are:
            // dueDate, assignedTo, folder
            f => BadRequest,
            v => {
                Ok(views.html.tasks.item(
                    Task(7, v, false, Option(date("2011-12-01")), Option(null), Project.findById(project).get, "Urgent")
                ))
            }
        )
    }
    def update(task: Long) = Action { Ok }
    def delete(task: Long) = Action { Ok }


    // LAZY FOLDERS
    def addFolder(id: Long) = Action {
        Ok( views.html.tasks.folder( "New folder" ){ null }  )
    }
    def deleteFolder(id: Long, folder: String) = Action { Ok }
    def renameFolder(id: Long, folder: String) = Action { implicit request =>
        Form("name" -> text).bind.fold(
            f => BadRequest,
            v => {
                Ok( v )
            }
        )
    }

}

