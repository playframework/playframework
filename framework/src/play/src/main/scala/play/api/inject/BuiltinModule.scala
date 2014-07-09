/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.inject

import javax.inject.Provider
import play.api.{ Application, DefaultApplication, Environment }

class BuiltinModule extends Module {
  def bindings(env: Environment): Seq[Binding[_]] = Seq(
    Binding(
      BindingKey(classOf[Environment], Seq.empty),
      ProviderTarget(new Provider[Environment] {
        def get: Environment = env
      }),
      None,
      eager = false
    ),
    // bind ApplicationLifecycle
    // bind Plugins - eager
    Binding(
      BindingKey(classOf[Application], Seq.empty),
      ProviderTarget(new Provider[Application] {
        def get: Application = new DefaultApplication(
          env.rootPath,
          env.classLoader,
          None,
          env.mode
        )
      }),
      None,
      eager = false
    )
  )
}
