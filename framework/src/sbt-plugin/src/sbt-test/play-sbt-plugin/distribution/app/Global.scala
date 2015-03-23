import play.api._

object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    // If the app lasts for more than 10 seconds, shut it down, so that the scripted tests don't leak,
    // but be sure that we don't call System.exit while shutting down, as this will create a deadlock
    val thread = new Thread() {
      override def run() = {
        try {
          Thread.sleep(10000)
          // We won't reach here if the app shuts down normally, because an exception will be thrown
          println("Forcibly terminating JVM that hasn't been shut down")
          System.exit(1)
        } catch {
          case _: Throwable =>
        }
      }
    }
    thread.setDaemon(true)
    thread.start()
  }

}