package controllers

import play.api.mvc._
import play.api.mvc.Results._
import play.api.libs._

import scalax.io._

object Assets extends Controller {
    
    def at(path:String, file:String) = Action { implicit ctx =>
        application.resourceAsStream(path + "/" + file).map {
            is => Binary(is, length = Some(is.available), contentType = MimeTypes.forFileName(file).getOrElse("application/octet-stream"))
        }.getOrElse(NotFound)
    }

}

