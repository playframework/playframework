/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.db;

import play.api.db.TransactionIsolationLevel$;

import java.sql.Connection;

/**
 * An enumeration defines of isolation level that determines the degree to which one transaction must be isolated from resource or data modifications made by other operations.
 */
public enum TransactionIsolationLevel {

    ReadUncommitted(Connection.TRANSACTION_READ_UNCOMMITTED),

    ReadCommited(Connection.TRANSACTION_READ_COMMITTED),

    RepeatedRead(Connection.TRANSACTION_REPEATABLE_READ),

    Serializable(Connection.TRANSACTION_SERIALIZABLE);

    private final int id;

    TransactionIsolationLevel(final int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public play.api.db.TransactionIsolationLevel asScala() {
        return TransactionIsolationLevel$.MODULE$.apply(id);
    }

    public static TransactionIsolationLevel fromId(final int id) {
        for (TransactionIsolationLevel type : values()) {
            if (type.getId() == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Not a valid value for transaction isolation level. See java.sql.Connection for possible options.");
    }

}
