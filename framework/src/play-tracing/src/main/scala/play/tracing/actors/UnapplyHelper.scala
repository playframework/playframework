/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package play.tracing.actors

trait UnapplyHelper[+T] {
  def unapply(in: Any): Option[T]
}
