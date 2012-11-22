
import play.api.GlobalSettings
import play.api.mvc.{RequestHeader, Result}
import play.api.mvc.Results.InternalServerError
import play.api.{Logger, Configuration}
import com.typesafe.config.ConfigFactory

object Global extends GlobalSettings {
  override def onError(r: RequestHeader, e: Throwable): Result = {
    InternalServerError("Something went wrong.")
  }

  // Ensure that the Evolutions code uses the same configuration as the running application
  // See: https://play.lighthouseapp.com/projects/82401-play-20/tickets/844
  override def configuration = {
    
    val extraConfig = 
      """
        |applyEvolutions.mock=false
        |db.mock.driver=org.h2.Driver
        |db.mock.url="jdbc:h2:mem:mock"
      """.stripMargin
    
    Configuration(ConfigFactory.parseString(extraConfig))
  }  
}