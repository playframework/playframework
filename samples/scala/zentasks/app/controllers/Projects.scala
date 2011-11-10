package controllers

import play.api._
import play.api.mvc._
import play.api.data._

import anorm._

import models._
import views._

/**
 * Manage projects related operations.
 */
object Projects extends Controller with Secured {

  /**
   * Display the dashboard.
   */
  def index = Action { request =>
    Ok(
      html.dashboard(
        Project.findInvolving(request.username.get), 
        Task.findTodoInvolving(request.username.get), 
        User.findByEmail(request.username.get).get
      )
    )
  }

  // -- Projects

  /**
   * Add a project.
   */
  def add = Action { implicit request =>
    Form("group" -> requiredText).bindFromRequest.fold(
      errors => BadRequest,
      folder => Ok(
        views.html.projects.item(
          Project.create(
            Project(NotAssigned, folder, "New project"), 
            Seq(request.username.get)
          )
        )
      )
    )
  }

  /**
   * Delete a project.
   */
  def delete(project: Long) = Action { implicit request =>
    IsMemberOf(project) {
      Project.delete(project)
      Ok
    }
  }

  /**
   * Rename a project.
   */
  def rename(project: Long) = Action { implicit request =>
    IsMemberOf(project) {
      Form("name" -> requiredText).bindFromRequest.fold(
        errors => BadRequest,
        newName => { Project.rename(project, newName); Ok(newName) }
      )
    }
  }

  // -- Project groups

  /**
   * Add a new project group.
   */
  def addGroup = Action {
    Ok(html.projects.group("New group"))
  }

  /**
   * Delete a project group.
   */
  def deleteGroup(folder: String) = Action { 
    Project.deleteInFolder(folder)
    Ok
  }

  /**
   * Rename a project group.
   */
  def renameGroup(folder: String) = Action { implicit request =>
    Form("name" -> requiredText).bindFromRequest.fold(
      errors => BadRequest,
      newName => { Project.renameFolder(folder, newName); Ok(newName) }
    )
  }

  // -- Members

  /**
   * Add a project member.
   */
  def addUser(project: Long) = Action { implicit request =>
    IsMemberOf(project) {
      Form("user" -> requiredText).bindFromRequest.fold(
        errors => BadRequest,
        user => { Project.addMember(project, user); Ok }
      )
    }
  }

  /**
   * Remove a project member.
   */
  def removeUser(project: Long) = Action { implicit request =>
    IsMemberOf(project) {
      Form("user" -> requiredText).bindFromRequest.fold(
        errors => BadRequest,
        user => { Project.removeMember(project, user); Ok }
      )
    }
  }

}

