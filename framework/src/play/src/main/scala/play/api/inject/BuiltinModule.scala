/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import play.api.{ Configuration, Application, DefaultApplication, Environment }

class BuiltinModule extends Module {
  def bindings(env: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[Environment] to env,
    bind[Configuration] to configuration,
    bind[Application].to[DefaultApplication]
  // bind ApplicationLifecycle
  // bind Plugins - eager
  )
}
