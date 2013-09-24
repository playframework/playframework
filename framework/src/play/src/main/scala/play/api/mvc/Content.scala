/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.mvc

/**
 * Generic type representing content to be sent over an HTTP response.
 */
trait Content {

  /**
   * The content String.
   */
  def body: String

  /**
   * The default Content type to use for this content.
   */
  def contentType: String

}