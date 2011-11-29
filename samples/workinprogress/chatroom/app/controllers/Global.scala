import play.api._

import actors._
import akka.actor._
import java.util.concurrent._

object MyGlobal extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("Start ChatRoomActor")
    ChatRoomActor.ref.start()
  }
  
  override def onStop(app: Application) {
    Logger.info("Stop ChatRoomActor")
    ChatRoomActor.ref.stop()
  }
  
}
