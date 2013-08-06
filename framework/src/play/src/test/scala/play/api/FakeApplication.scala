/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api

import play.core.SourceMapper
import java.io.File

/**
 * Fake application as used by Play core tests.  This is needed since Play core can't depend on the Play test API.
 * It's also a lot simpler, doesn't load default config files etc.
 */
case class FakeApplication(config: Map[String, Any] = Map(),
                           path: File = new File("."),
                           sources: Option[SourceMapper] = None,
                           mode: Mode.Mode = Mode.Test,
                           withGlobal: GlobalSettings = DefaultGlobal,
                           withPlugins: Seq[Plugin] = Seq()) extends Application {
  lazy val injectionProvider = new DefaultInjectionProvider(this) {
    override lazy val plugins = withPlugins
    override lazy val global = withGlobal
  }
  val classloader = Thread.currentThread.getContextClassLoader
  lazy val configuration = Configuration.from(config)
}
