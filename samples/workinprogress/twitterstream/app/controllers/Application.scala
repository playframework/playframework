package controllers

import views._

import play.Logger
import play.api._
import play.api.libs.ws._
import play.api.libs.oauth._
import play.api.mvc._
import play.api.libs._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import com.ning.http.client.Realm.AuthScheme

object Application extends Controller {

  // Application

  def index = Security.Authenticated(
    request => request.session.get("token"),
    _ => Results.Redirect(routes.Twitter.authenticate))(username => Action(Ok(html.index())))

  def twitter(term: String) = Action { request =>
    val tokens = Twitter.sessionTokenPair(request).get
    val toComet = Enumeratee.map[Array[Byte]](bytes => new String(bytes)) ><> Comet(callback = "window.parent.twitts")(Comet.CometMessage(identity))

    Ok.stream { socket: Socket.Out[play.api.templates.Html] =>
      WS.url("https://stream.twitter.com/1/statuses/filter.json?track=" + term)
        .sign(OAuthCalculator(Twitter.KEY, tokens))
        .get(res => toComet &> socket)
    }
  }

}

