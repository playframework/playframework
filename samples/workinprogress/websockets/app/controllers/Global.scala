import play.api._

import actors._
import akka.actor._
import java.util.concurrent._

object MyGlobal extends GlobalSettings {
  
  override def onStart(app: Application) {
    Logger.info("Start HelloActor")
    HelloActor.ref.start()
    /*Scheduler.schedule(HelloActor.ref, 
      """Hello""".stripMargin
      , 0, 1, TimeUnit.SECONDS)*/
  }
  
  override def onStop(app: Application) {
    Logger.info("Stop HelloActor")
    HelloActor.ref.stop()
  }
  
}