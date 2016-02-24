/*
 * Copyright (C) 2009-2016 Lightbend Inc. <https://www.lightbend.com>
 */
package play.api

/**
 * Contains the JDBC database access API.
 *
 * Example, retrieving a connection from the 'customers' datasource:
 * {{{
 * val conn = DB.getConnection("customers")
 * }}}
 */
package object db {
  type NamedDatabase = play.db.NamedDatabase
}
