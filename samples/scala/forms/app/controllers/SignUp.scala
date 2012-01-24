package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._

import views._

import models._

object SignUp extends Controller {
  
  /**
   * Sign Up Form definition.
   *
   * Once defined it handle automatically, ,
   * validation, submission, errors, redisplaying, ...
   */
  val signupForm: Form[User] = Form(
    
    // Define a mapping that will handle User values
    mapping(
      "username" -> text(minLength = 4),
      "email" -> email,
      
      // Create a tuple mapping for the password/confirm
      "password" -> tuple(
        "main" -> text(minLength = 6),
        "confirm" -> text
      ).verifying(
        // Add an additional constraint: both passwords must match
        "Passwords don't match", passwords => passwords._1 == passwords._2
      ),
      
      // Create a mapping that will handle UserProfile values
      "profile" -> mapping(
        "country" -> nonEmptyText,
        "address" -> optional(text),
        "age" -> optional(number(min = 18, max = 100))
      )
      // The mapping signature matches the UserProfile case class signature,
      // so we can use default apply/unapply functions here
      (UserProfile.apply)(UserProfile.unapply),
      
      "accept" -> checked("You must accept the conditions")
      
    )
    // The mapping signature doesn't match the User case class signature,
    // so we have to define custom binding/unbinding functions
    {
      // Binding: Create a User from the mapping result (ignore the second password and the accept field)
      (username, email, passwords, profile, _) => User(username, passwords._1, email, profile) 
    } 
    {
      // Unbinding: Create the mapping values from an existing User value
      user => Some(user.username, user.email, (user.password, ""), user.profile, false)
    }.verifying(
      // Add an additional constraint: The username must not be taken (you could do an SQL request here)
      "This username is not available",
      user => !Seq("admin", "guest").contains(user.username)
    )
  )
  
  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.signup.form(signupForm));
  }
  
  /**
   * Display a form pre-filled with an existing User.
   */
  def editForm = Action {
    val existingUser = User(
      "fakeuser", "secret", "fake@gmail.com", 
      UserProfile("France", None, Some(30))
    )
    Ok(html.signup.form(signupForm.fill(existingUser)))
  }
  
  /**
   * Handle form submission.
   */
  def submit = Action { implicit request =>
    signupForm.bindFromRequest.fold(
      // Form has errors, redisplay it
      errors => BadRequest(html.signup.form(errors)),
      
      // We got a valid User value, display the summary
      user => Ok(html.signup.summary(user))
    )
  }
  
}