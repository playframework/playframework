/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
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
