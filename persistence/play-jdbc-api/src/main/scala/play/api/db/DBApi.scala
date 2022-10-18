/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

/**
 * DB API for managing application databases.
 */
trait DBApi {

  /**
   * All configured databases.
   */
  def databases(): Seq[Database]

  /**
   * Get database with given configuration name.
   *
   * @param name the configuration name of the database
   */
  def database(name: String): Database

  /**
   * Shutdown all databases, releasing resources.
   */
  def shutdown(): Unit
}
