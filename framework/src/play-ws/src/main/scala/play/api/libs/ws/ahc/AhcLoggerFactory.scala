/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.libs.ws.ahc

import com.typesafe.sslconfig.util.{ LoggerFactory, NoDepsLogger }
import org.slf4j.{ LoggerFactory => SLF4JLoggerFactory }

class AhcLoggerFactory extends LoggerFactory {

  private[ahc] def createLogger(name: String) = {
    new NoDepsLogger {
      private[ahc] val logger = SLF4JLoggerFactory.getLogger(this.getClass)

      def warn(msg: String): Unit = logger.warn(msg)
      def isDebugEnabled: Boolean = logger.isDebugEnabled
      def error(msg: String): Unit = logger.error(msg)
      def error(msg: String, throwable: Throwable): Unit = logger.error(msg, throwable)
      def debug(msg: String): Unit = logger.debug(msg)
      def info(msg: String): Unit = logger.info(msg)
    }
  }

  def apply(clazz: Class[_]): NoDepsLogger = createLogger(clazz.getName)
  def apply(name: String): NoDepsLogger = createLogger(name)

}
