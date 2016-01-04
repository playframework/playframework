/*
 * Copyright (C) 2009-2016 Typesafe Inc. <http://www.typesafe.com>
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