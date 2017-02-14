/*
 * Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

/**
 * Contains test helpers.
 */
package object test {
  /**
   * Provided as an implicit by WithServer and WithBrowser.
   */
  type Port = Int

  /**
   * A structural type indicating there is an port.
   */
  type HasPort = {
    def port: Port
  }

  /**
   * A structural type indicating there is an application.
   */
  type HasApp = {
    def app: Application
  }

  /**
   * A structural type indicating that there is an application and a port.
   */
  type HasAppAndPort = HasApp with HasPort

}
