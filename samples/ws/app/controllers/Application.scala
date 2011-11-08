package controllers

import play.api._
import play.api.mvc._

import com.ning.http.client._

object Application extends Controller {

  def index = Action {
    val response = WS.url("http://google.com").get()
    AsyncResult(response.map(r => Ok(r.getResponseBody())))
  }

}
