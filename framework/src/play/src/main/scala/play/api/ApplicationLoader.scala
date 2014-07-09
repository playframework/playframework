/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.api.inject.guice.GuiceApplicationLoader

object ApplicationLoader {
  def apply(env: Environment): ApplicationLoader = {
    env.configuration.getString("application.loader").fold[ApplicationLoader](new GuiceApplicationLoader) { loaderClass =>
      try {
        env.classLoader.loadClass(loaderClass).newInstance.asInstanceOf[ApplicationLoader]
      } catch {
        case e: VirtualMachineError => throw e
        case e: ThreadDeath => throw e
        case e: Throwable => throw new PlayException(
          "Cannot load ApplicationLoader",
          "ApplicationLoader [" + loaderClass + "] cannot be instantiated.",
          e)
      }
    }
  }
}

trait ApplicationLoader {
  def load(env: Environment): Application
}
