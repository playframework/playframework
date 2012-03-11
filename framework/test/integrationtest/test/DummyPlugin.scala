package test

import play.api._

class DummyPlugin(app: Application) extends Plugin {
  
  lazy val foo = "yay"

  override def onStart() {
    foo
  }

}