import play.api._
import play.api.mvc._

object Global extends GlobalSettings {
    
    override def onStart(app:Application) {
        println("Application start")
    }

} 
