/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api.db

/**
 * DB components for compile-time DI
 */
trait DBComponents {
  def dbApi: DBApi
}
