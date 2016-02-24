/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil
import play.api.libs.logback.ColoredLevel

/**
 * Provides the old play.api.Logger$ColoredLevel library so that a deprecation message can be logged.
 */
@deprecated("Use play.api.libs.logback.ColoredLevel instead", "2.5.0")
class Logger$ColoredLevel extends ColoredLevel {

  override def start(): Unit = {
    super.start()
    val configLocation = Option(ConfigurationWatchListUtil.getMainWatchURL(getContext)).fold("your logback configuration")(_.toString)
    val migrationDocs = "https://www.playframework.com/documentation/2.5.x/Migration25#Change-to-Logback-configuration"
    addError(s"You are using the deprecated ${this.getClass.getName} in $configLocation, please use ${classOf[ColoredLevel].getName} instead. See $migrationDocs for more information.")
  }
}
