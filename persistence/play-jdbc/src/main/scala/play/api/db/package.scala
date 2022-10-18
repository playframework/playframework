/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api

/**
 * Contains the JDBC database access API.
 *
 * Example, retrieving a connection from the 'customers' datasource:
 * {{{
 * val conn = db.getConnection("customers")
 * }}}
 */
package object db {
  type NamedDatabase = play.db.NamedDatabase
}
