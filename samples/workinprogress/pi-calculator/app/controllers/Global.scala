import play.api._

import java.util.concurrent._

object MyGlobal extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("Start Pi-Calc")
  }
  
  override def onStop(app: Application) {
    Logger.info("Stop Actor System")
    controllers.Application.system.shutdown
  }
  
}
