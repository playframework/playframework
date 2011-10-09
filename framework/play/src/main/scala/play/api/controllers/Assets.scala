package controllers

import play.api._
import play.api.mvc._
import play.api.libs._

import Play.currentApplication

import scalax.io._

object Assets extends Controller {
    
    def at(path: String, file: String) = Action {
        Play.resourceAsStream(path + "/" + file).map {
            is => Binary(is, length = Some(is.available), contentType = MimeTypes.forFileName(file).getOrElse("application/octet-stream"))
        }.getOrElse(NotFound)
    }

}

