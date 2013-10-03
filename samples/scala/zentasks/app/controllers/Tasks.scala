package controllers

import play.api._
import play.api.mvc._

import play.api.data.mapping._
import Rules._

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
  def index(project: Long) = IsMemberOf(project) { _ => implicit request =>
    Project.findById(project).map { p =>
      val tasks = Task.findByProject(project)
      val team = Project.membersOf(project)
      Ok(html.tasks.index(p, tasks, team))
    }.getOrElse(NotFound)
  }

  val taskValidation = From[UrlFormEncoded] { __ =>
    import Rules._
    ((__ \ "title").read(notEmpty) ~
     (__ \ "dueDate").read(option(date("MM/dd/yy"))) ~
     (__ \ "assignedTo").read[Option[String]]).tupled
  }

  // -- Tasks

  /**
   * Create a task in this project.
   */
  def add(project: Long, folder: String) =  IsMemberOf(project) { _ => implicit request =>
    taskValidation
      .validate(request.body.asFormUrlEncoded.getOrElse(Map.empty))
      .fold(
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

  /**
   * Update a task
   */
  def update(task: Long) = IsOwnerOf(task) { _ => implicit request =>
    (Path \ "done").read(boolean)
      .validate(request.body.asFormUrlEncoded.getOrElse(Map.empty))
      .fold(
        errors => BadRequest,
        isDone => {
          Task.markAsDone(task, isDone)
          Ok
        }
      )
  }

  /**
   * Delete a task
   */
  def delete(task: Long) = IsOwnerOf(task) { _ => implicit request =>
    Task.delete(task)
    Ok
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
  def deleteFolder(project: Long, folder: String) = IsMemberOf(project) { _ => implicit request =>
    Task.deleteInFolder(project, folder)
    Ok
  }

  /**
   * Rename a tasks folder.
   */
  def renameFolder(project: Long, folder: String) = IsMemberOf(project) { _ => implicit request =>
    (Path \ "name").read(notEmpty)
      .validate(request.body.asFormUrlEncoded.getOrElse(Map.empty))
      .fold(
        errors => BadRequest,
        newName => {
          Task.renameFolder(project, folder, newName)
          Ok(newName)
        }
      )
  }

}

