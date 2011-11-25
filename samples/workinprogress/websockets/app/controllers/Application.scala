package controllers

import play.api._
import play.api.mvc._

import play.api.libs.iteratee._
import play.api.libs.concurrent._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index()) 
  }
  
  def echo(name: String) = WebSocket[String] { request => (in, out) =>
    
    Logger.info(name + " is connected!")
    
    out <<: in.map {
      case EOF => {
        Logger.info(name + " is disconnected. Cleaning resources")
        EOF
      }
      case el => {
        Logger.info("Got message: " + el)
        el.map("[" + name + "] " + _.reverse)
      }
    }    
  }
  
  def stream = Action {
    AsyncResult {
      new AkkaPromise(actors.HelloActor.ref ? "join" map {_.asInstanceOf[Enumerator[String]]}).map { chunks =>
        Ok(Enumerator(Array.fill[Char](5000)(' ').mkString + """<html><body>""").andThen(chunks.map { in =>
          in.map { msg =>
            """
              <script type="text/javascript">console.log('""" + msg + """')</script>
            """
          }
        })).as(HTML)
      }
    }
  }
  
  def say(message: String) = Action {
    actors.HelloActor.ref ! message
    Ok
  }
  
}