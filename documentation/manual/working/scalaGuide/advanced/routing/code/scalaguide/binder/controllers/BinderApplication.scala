package scalaguide.binder.controllers


import play.api._
import play.api.mvc._
import scalaguide.binder.models._

class BinderApplication extends Controller{
 
  //#path
  def user(user: User) = Action {
    Ok(user.name)
  }
  //#path

  //#query
  def age(age: AgeRange) = Action {
    Ok(age.from.toString)
  }
  //#query

}
