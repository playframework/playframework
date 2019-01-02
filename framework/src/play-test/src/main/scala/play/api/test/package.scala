/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
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
   * A structural type indicating there is an application.
   */
  type HasApp = {
    def app: Application
  }

}
