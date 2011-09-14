import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
    
    override def onStart {
        println("Application start")
    }
    
}