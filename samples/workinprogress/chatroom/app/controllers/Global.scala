import play.api._

import actors._
import java.util.concurrent._

object MyGlobal extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("Start ChatRoomActor")
  }
  
  override def onStop(app: Application) {
    Logger.info("Stop ChatRoomActor")
    ChatRoomActor.system.shutdown
  }
  
}
