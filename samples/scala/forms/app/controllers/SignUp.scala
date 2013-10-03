package controllers

import play.api._
import play.api.mvc._

import play.api.data.mapping._
import play.api.libs.functional.syntax._

import views._

import models._

object SignUp extends Controller {

  implicit val profileValidation = From[Map[String, Seq[String]]] { __ =>
    import Rules._
    // Create a validation that will handle UserProfile values
    ((__ \ "country").read(notEmpty) ~
     (__ \ "address").read[Option[String]] ~
     (__ \ "age").read(option(min(18) |+| max(100)))) (UserProfile.apply _)
  }

  implicit val profileW = To[Map[String, Seq[String]]] { __ =>
    import Writes._
    // Create a validation that will handle UserProfile values
    ((__ \ "country").write[String] ~
     (__ \ "address").write[Option[String]] ~
     (__ \ "age").write[Option[Int]]) (unlift(UserProfile.unapply _))
  }

  // Nn additional constraint: The username must not be taken (you could do an SQL request here)
  val isAvailable = Rules.validateWith[User]("This username is not available"){ user =>
    !Seq("admin", "guest").contains(user.username)
  }

  implicit val signupValidation = From[Map[String, Seq[String]]] { __ =>
    import Rules._
    (
      // Define a mapping that will handle User values
      (__ \ "username").read(minLength(4)) ~

      // Create a tuple mapping for the password/confirm
      (__ \ "password").read(
        ((__ \ "main").read(minLength(6)) ~
         (__ \ "confirm").read[String]).tupled
      )
      // Add an additional constraint: both passwords must match
      .compose(Rule.uncurry(equalTo[String])) ~

      (__ \ "email").read(email) ~

      (__ \ "profile").read[UserProfile] ~
      (__ \ "accept").read(checked)
    )((username, password, email, profile, _) => User(username, password, email, profile))
  }.compose(isAvailable)

  implicit val userW: Write[User, Map[String, Seq[String]]] = To[Map[String, Seq[String]]] { __ =>
    import Writes._
    // Create a validation that will handle UserProfile values
    ((__ \ "username").write[String] ~
     (__ \ "password").write(
      ((__ \ "main").write[String] ~
       (__ \ "confirm").write[String]).tupled
     ) ~
     (__ \ "email").write[String] ~
     (__ \ "profile").write[UserProfile] ~
     (__ \ "accept").write[Boolean]) (u => (u.username, ("", ""), u.email, u.profile, true))
  }

  /**
   * Display an empty form.
   */
  def form = Action {
    Ok(html.signup.form(Form()));
  }

  /**
   * Display a form pre-filled with an existing User.
   */
  def editForm = Action {
    val existingUser = User(
      "fakeuser", "secret", "fake@gmail.com",
      UserProfile("France", None, Some(30))
    )
    Ok(html.signup.form(Form.fill(existingUser)))
  }

  /**
   * Handle form submission.
   */
  def submit = Action(parse.urlFormEncoded) { implicit request =>
    val r = signupValidation.validate(request.body)
    r.fold(
      // Form has errors, redisplay it
      errors => BadRequest(html.signup.form(Form(request.body, r))),
      // We got a valid User value, display the summary
      user => Ok(html.signup.summary(user))
    )
  }

}