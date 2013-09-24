/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
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
package object db