/*
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import java.sql.Connection

/**
 * An enumeration defines of isolation level that determines the degree to which one transaction must be isolated from resource or data modifications made by other operations.
 */
object TransactionIsolationLevel extends Enumeration {
  type TransactionIsolationLevel = Value

  val ReadUncommitted: Value = Value(Connection.TRANSACTION_READ_UNCOMMITTED)
  val ReadCommited: Value = Value(Connection.TRANSACTION_READ_COMMITTED)
  val RepeatedRead: Value = Value(Connection.TRANSACTION_REPEATABLE_READ)
  val Serializable: Value = Value(Connection.TRANSACTION_SERIALIZABLE)

  //sealed abstract class TransactionIsolationLevel(val id: Int)
  //
  //object TransactionIsolationLevel {
  //
  //  case object ReadUncommitted extends TransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)
  //  case object ReadCommited extends TransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED)
  //  case object RepeatedRead extends TransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)
  //  case object Serializable extends TransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE)
  //
  //}

}
