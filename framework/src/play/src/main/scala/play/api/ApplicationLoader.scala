/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.inject.guice.GuiceApplicationLoader
import play.utils.Reflect

trait ApplicationLoader {
  def load(env: Environment): Application
}

object ApplicationLoader {
  def apply(env: Environment): ApplicationLoader = {
    env.configuration.getString("application.loader").fold[ApplicationLoader](new GuiceApplicationLoader) { loaderClass =>
      Reflect.createInstance[ApplicationLoader](loaderClass, env.classLoader)
    }
  }
}
