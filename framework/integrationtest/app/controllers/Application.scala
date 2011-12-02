package controllers

import play.api.mvc._
import play.api.cache.Cache
import play.cache.{Cache=>JCache}

import play.api.Json._
import models._
import models.Protocol._

object Application extends Controller {

  def index = Action {
    import play.api.Play.current
    Cache.set("hello","world")
    val v2 = Cache.get[String]("peter","hello")
    if (v2("peter") == "world" && v2("hello") == "world") throw new RuntimeException("scala cache API is not working")
    Ok(views.html.index(Cache.get[String]("hello").getOrElse("oh noooz")))
  }

  def post = Action {
    Ok(views.html.index("POST!"))
  }

  def json = Action {
    Ok(tojson(User(1, "Sadek", List("tea"))))
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

