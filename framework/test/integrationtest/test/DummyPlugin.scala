/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package test

import play.api._

class DummyPlugin(app: Application) extends Plugin {
  
  lazy val foo = "yay"

  override def onStart() {
    foo
  }

}