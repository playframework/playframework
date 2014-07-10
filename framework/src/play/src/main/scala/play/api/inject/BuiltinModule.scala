/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import javax.inject.Provider
import play.api.{ Application, DefaultApplication, Environment }

class BuiltinModule extends Module {
  def bindings(env: Environment): Seq[Binding[_]] = Seq(
    bind[Environment] to env,
    // bind ApplicationLifecycle
    // bind Plugins - eager
    bind[Application] to new DefaultApplication(env.rootPath, env.classLoader, None, env.mode)
  )
}
