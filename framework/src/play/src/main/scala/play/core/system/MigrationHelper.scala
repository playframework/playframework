/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.core.system

import play.api.{ Play, Plugin, Application }

/**
 * The migration helper is designed to assist in migration, where some things, such as configuration, have changed in
 * ways that break silently.
 */
class MigrationHelper(app: Application) extends Plugin {

  override def onStart() = {
    checkSessionMaxAge
  }

  def checkSessionMaxAge = {
    app.configuration.getString("session.maxAge").map { maxAge =>
      if (maxAge.matches("\\d+")) {
        // It doesn't have a time unit, check that it's a sane number (greater than 10 minutes)
        if (maxAge.toLong < 600000l) {
          Play.logger.warn(
            s"""
              |In Play 2.3, session.maxAge was changed from being an integer for the number of seconds to being a
              |duration. This means you can now specify time units, for example 1h, or 30m etc. If however, no time
              |unit is specified, it defaults to milliseconds, making this a breaking change. The configured value
              |in this application ($maxAge) does not have a time unit, and is suspiciously low for a session
              |timeout, you may need to update your configuration. To prevent this warning message from showing in
              |future, either add a time unit to the session.maxAge configuration item (eg, ms), or disable the
              |migration helper plugin using play.migrationhelper=disabled.
            """.stripMargin)
        }
      }
    }
  }

  override def enabled = app.configuration.getString("play.migrationhelper").forall(_ != "disabled")
}
