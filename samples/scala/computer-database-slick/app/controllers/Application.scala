package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import play.api.db.slick.mvc._

import views._
import models._

/**
 * Manage a database of computers
 */
object Application extends Controller with SlickController { 
  
  /**
   * This result directly redirect to the application home.
   */
  val Home = Redirect(routes.Application.list(0, 2, ""))
  
  /**
   * Describe the computer form (used in both edit and create screens).
   */ 
  val computerForm = Form(
    mapping(
      "id" -> optional(longNumber),
      "name" -> nonEmptyText,
      "introduced" -> optional(date("yyyy-MM-dd")),
      "discontinued" -> optional(date("yyyy-MM-dd")),
      "company" -> optional(longNumber)
    )(Computer.apply)(Computer.unapply)
  )
  
  // -- Actions

  /**
   * Handle default path requests, redirect to computers list
   */  
  def index = Action { Home }
  
  /**
   * Display the paginated list of computers.
   *
   * @param page Current page number (starts from 0)
   * @param orderBy Column to be sorted
   * @param filter Filter applied on computer names
   */
  def list(page: Int, orderBy: Int, filter: String) = SlickAction { implicit request =>
    Ok(html.list(
      Computers.list(page = page, orderBy = orderBy, filter = ("%"+filter+"%")),
      orderBy, filter
    ))
  }
  
  /**
   * Display the 'edit form' of a existing Computer.
   *
   * @param id Id of the computer to edit
   */
  def edit(id: Long) = SlickAction {
    Computers.findById(id).map { computer =>
      Ok(html.editForm(id, computerForm.fill(computer), Companies.options))
    }.getOrElse(NotFound)
  }
  
  /**
   * Handle the 'edit form' submission 
   *
   * @param id Id of the computer to edit
   */
  def update(id: Long) = SlickAction { implicit request =>
    computerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.editForm(id, formWithErrors, Companies.options)),
      computer => {
        Computers.update(id, computer)
        Home.flashing("success" -> "Computer %s has been updated".format(computer.name))
      }
    )
  }
  
  /**
   * Display the 'new computer form'.
   */
  def create = SlickAction {
    Ok(html.createForm(computerForm, Companies.options))
  }
  
  /**
   * Handle the 'new computer form' submission.
   */
  def save = SlickAction { implicit request =>
    computerForm.bindFromRequest.fold(
      formWithErrors => BadRequest(html.createForm(formWithErrors, Companies.options)),
      computer => {
        Computers.insert(computer)
        Home.flashing("success" -> "Computer %s has been created".format(computer.name))
      }
    )
  }
  
  /**
   * Handle computer deletion.
   */
  def delete(id: Long) = SlickAction {
    Computers.delete(id)
    Home.flashing("success" -> "Computer has been deleted")
  }

}
            
