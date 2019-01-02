/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import java.sql.Connection

/**
 * Defines isolation levels that determines the degree to which one transaction must be isolated from resource or data modifications made by other operations.
 *
 * @param id the transaction isolation level.
 *
 * @see [[Connection]].
 */
sealed abstract class TransactionIsolationLevel(val id: Int) {
  def asJava(): play.db.TransactionIsolationLevel = play.db.TransactionIsolationLevel.fromId(id)
}

object TransactionIsolationLevel {

  case object ReadUncommitted extends TransactionIsolationLevel(Connection.TRANSACTION_READ_UNCOMMITTED)

  case object ReadCommited extends TransactionIsolationLevel(Connection.TRANSACTION_READ_COMMITTED)

  case object RepeatedRead extends TransactionIsolationLevel(Connection.TRANSACTION_REPEATABLE_READ)

  case object Serializable extends TransactionIsolationLevel(Connection.TRANSACTION_SERIALIZABLE)

  def apply(id: Int): TransactionIsolationLevel = id match {
    case Connection.TRANSACTION_READ_COMMITTED => ReadCommited
    case Connection.TRANSACTION_READ_UNCOMMITTED => ReadUncommitted
    case Connection.TRANSACTION_REPEATABLE_READ => RepeatedRead
    case Connection.TRANSACTION_SERIALIZABLE => Serializable
    case _ => throw new IllegalArgumentException("Not a valid value for transaction isolation level. See java.sql.Connection for possible options.")
  }

}