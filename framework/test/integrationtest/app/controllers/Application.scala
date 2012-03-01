package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.Configuration

import play.api.cache.Cache
import play.api.libs.json._
import play.api.libs.json.Json._

import models._
import models.Protocol._

import play.cache.{Cache=>JCache}


object Application extends Controller {

  def index = Action {
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

    val s = config.getString("complex-app.something").getOrElse("boooooo")
    val c = config.getString("nokey").getOrElse("None")
    val overrideAkka = play.api.libs.concurrent.Akka.system.settings.config.getString("akka.loglevel")
    Ok(s + " no key: " + c +" - override akka:"+ overrideConfig+" akka-loglevel:"+ overrideAkka)
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

  def index_java_cache = Action {
    import play.api.Play.current
    JCache.set("hello","world", 60)
    JCache.set("peter","world", 60)
    val v = JCache.get("hello")
    if (v != "world") throw new RuntimeException("java cache API is not working")
    Ok(views.html.index(Cache.get("hello").map(_.toString).getOrElse("oh noooz")))
  }

}

