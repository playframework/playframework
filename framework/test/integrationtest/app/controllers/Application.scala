package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.Configuration

import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.libs.Jsonp
import play.api.libs.concurrent.Promise

import models._
import models.Protocol._

import play.cache.{Cache=>JCache}
import play.api.i18n._

object Application extends Controller {

  def index = Action {
    if (Messages("home.title")(Lang("fr")) != "ffff" ) throw new RuntimeException("i18n does not work")
    if (Messages("constraint.required") != "Hijacked" ) throw new RuntimeException("can not override default message")
    val conn = play.api.db.DB.getConnection("default")
    Cache.set("hello", "world")
    Ok(views.html.index(Cache.getAs[String]("hello").getOrElse("oh noooz")))
  }

  def submitForm = Action{
   Ok("ok")
  }

  def form = Action{
    Ok(views.html.form(Contacts.form.fill(Contact("","M"))));
  }

  def conf = Action {
    val config = play.api.Play.configuration
    val overrideConfig =  play.api.Configuration.load(new java.io.File(".")).getString("play.akka.actor.retrieveBodyParserTimeout").get
    val timeout = config.getMilliseconds("promise.akka.actor.typed.timeout").get

    val s = config.getString("complex-app.something").getOrElse("boooooo")
    val c = config.getString("nokey").getOrElse("None")
    val overrideAkka = play.api.libs.concurrent.Akka.system.settings.config.getString("akka.loglevel")
    Ok(s + " no key: " + c +" - override akka:"+ overrideConfig+" akka-loglevel:"+ overrideAkka+ " promise-timeout:"+ timeout)
  }
  
  def post = Action { request =>
    val content: String = request.body.toString
    Ok(views.html.index(content))
  }

  def json = Action {
    Ok(toJson(User(1, "Sadek", List("tea"))))
  }
  
  def jsonFromJsObject = Action {
    Ok(toJson(JsObject(List("blah" -> JsString("foo"))))) 
  }

  def jsonWithContentType = Action { request =>
    request.headers.get("AccEPT") match {
      case Some("application/json") =>  {
        val acceptHdr = request.headers.toMap.collectFirst{ case (header,valueSeq) if header.equalsIgnoreCase("Accept") => (header, valueSeq) }
        acceptHdr.map{
          case (name,value) => Ok("{\""+name+"\":\""+ value.head+ "\"}").as("application/json")
        }.getOrElse(InternalServerError)
      }
      case _ => UnsupportedMediaType

    }
  }

  def jsonWithContentTypeAndCharset = Action {
    Ok("{}").as("application/json; charset=utf-8")
  }

  def index_java_cache = Action {
    import play.api.Play.current
    JCache.set("hello","world", 60)
    JCache.set("peter","world", 60)
    val v = JCache.get("hello")
    if (v != "world") throw new RuntimeException("java cache API is not working")
    Ok(views.html.index(Cache.get("hello").map(_.toString).getOrElse("oh noooz")))
  }

  def takeBool(b: Boolean) = Action {
    Ok(b.toString())
  }

  def takeBool2(b: Boolean) = Action {
    Ok(b.toString())
  }
  
  def javascriptRoutes = Action { implicit request =>
    import play.api.Routes
    Ok(Routes.javascriptRouter("routes")(routes.javascript.Application.javascriptTest)).as("text/javascript")
  }

  def javascriptTest(name: String) = Action {
    Ok(views.html.javascriptTest(name))
  }

  def takeList(xs: List[Int]) = Action {
    Ok(xs.mkString)
  }

  def jsonp(callback: String) = Action {
    val json = JsObject(List("foo" -> JsString("bar")))
    Ok(Jsonp(callback, json))
  }

  def urldecode(fromPath: String, fromQueryString: String) = Action {
    Ok("fromPath=%s fromQueryString=%s".format(fromPath, fromQueryString))
  }

  def accept = Action { request =>
    request match {
      case Accepts.Json() => Ok("json")
      case Accepts.Html() => Ok("html")
      case _ => BadRequest
    }
  }

  def onCloseSendFile(filepath: String) = Action {
    import java.io.File
    val file = new File(filepath)
    Ok.sendFile(file, onClose = () => { file.delete() })
  }

  def syncError = Action {
    sys.error("Error")
    Ok
  }

  def asyncError = Action {
    Async {
      Promise.pure[Result](sys.error("Error"))
    }
  }
}
