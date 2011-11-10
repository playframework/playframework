package controllers

import play.api._
import play.api.mvc._
import play.api.data._

import java.util.{Date}

import anorm._

import models._
import views._

/**
 * Manage tasks related operations.
 */
object Tasks extends Controller with Secured {

  /**
   * Display the tasks panel for this project.
   */
  def index(project: Long) = Action { implicit request =>
    Project.findById(project).map { p =>
      IsMemberOf(project) {
        val tasks = Task.findByProject(project)
        val team = Project.membersOf(project)
        Ok(html.tasks.index(p, tasks, team))
      }
    }.getOrElse(NotFound)
  }

  val taskForm = Form(
    of(
      "title" -> requiredText,
      "dueDate" -> optional(date("MM/dd/yy")),
      "assignedTo" -> optional(text)
    )
  )

  // -- Tasks

  /**
   * Create a task in this project.
   */  
  def add(project: Long, folder: String) = Action { implicit request =>
    IsMemberOf(project) {
      taskForm.bindFromRequest.fold(
        errors => BadRequest,
        {
          case (title, dueDate, assignedTo) => 
            val task =  Task.create(
              Task(NotAssigned, folder, project, title, false, dueDate, assignedTo)
            )
            Ok(html.tasks.item(task))
        }
      )
    }
  }

  /**
   * Update a task
   */
  def update(task: Long) = Action { implicit request =>
    IsOwnerOf(task) {
      Form("done" -> boolean).bindFromRequest.fold(
        errors => BadRequest,
        isDone => { Task.markAsDone(task, isDone); Ok }
      )
    }
  }

  /**
   * Delete a task
   */
  def delete(task: Long) = Action { implicit request =>
    IsOwnerOf(task) {
      Task.delete(task)
      Ok
    }
  }

  // -- Task folders

  /**
   * Add a new folder.
   */
  def addFolder = Action {
    Ok(html.tasks.folder("New folder"))
  }

  /**
   * Delete a full tasks folder.
   */
  def deleteFolder(project: Long, folder: String) = Action { implicit request =>
    IsMemberOf(project) {
      Task.deleteInFolder(project, folder)
      Ok
    }
  }

  /**
   * Rename a tasks folder.
   */
  def renameFolder(project: Long, folder: String) = Action { implicit request =>
    IsMemberOf(project) {
      Form("name" -> requiredText).bindFromRequest.fold(
        errors => BadRequest,
        newName => { Task.renameFolder(project, folder, newName); Ok(newName) }
      )
    }
  }

}

