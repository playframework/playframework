package controllers

import play.api.mvc._
import play.api.Play.current
import play.api.Configuration

import play.api.cache.Cache
import play.api.libs.json._

import models._
import models.Protocol._

import play.cache.{Cache=>JCache}

object Application extends Controller {

  def index = Action {
    val conn = play.api.db.DB.getConnection("default")
    Cache.set("hello","world")
    val v2 = Cache.get[String]("peter","hello")
    if (v2("peter") == "world" && v2("hello") == "world") throw new RuntimeException("scala cache API is not working")
    Ok(views.html.index(Cache.get[String]("hello").getOrElse("oh noooz")))
  }

  def conf = Action {
    val config = play.api.Play.configuration
    val s = config.getString("complex-app.something").getOrElse("boooooo")
    val c = config.getString("nokey").getOrElse("None")
    Ok(s + " no key: " + c)
  }
  
  def post = Action {
    Ok(views.html.index("POST!"))
  }

  def json = Action {
    Ok(toJson(User(1, "Sadek", List("tea"))))
  }
  def jsonFromJsObject = Action {
    Ok(toJson(JsObject(List("blah" -> JsString("foo"))))) 
  }

  def index_java_cache = Action {
    import play.api.Play.current
    JCache.set("hello","world")
    JCache.set("peter","world")
    val v = JCache.get("hello")
    if (v != "world") throw new RuntimeException("java cache API is not working")
    val v2 = Cache.get[String]("peter","hello")
    if (v2("peter") == "world" && v2("hello") == "world") throw new RuntimeException("scala cache API is not working")
    Ok(views.html.index(Cache.get[String]("hello").getOrElse("oh noooz")))
  }

}

