/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.{ Inject, Provider, Singleton }

import scala.concurrent.Future
import scala.util.control.NonFatal

import play.api.inject.{ ApplicationLifecycle, Module }
import play.api.{ Application, Configuration, Environment, Logger, Mode, Play }
import play.db.NamedDatabaseImpl

/**
 * DB runtime inject module.
 */
class DBModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    if (configuration.underlying.getBoolean("play.modules.db.enabled")) {
      val dbs = configuration.getConfig("db").getOrElse(Configuration.empty).subKeys
      def named(name: String): NamedDatabase = new NamedDatabaseImpl(name)
      Seq(
        bind[DBApi].toProvider[DBApiProvider],
        bind[Database].to(bind[Database].qualifiedWith(named("default")))
      ) ++ dbs.map { db =>
          bind[Database].qualifiedWith(named(db)).to(new NamedDatabaseProvider(db))
        }
    } else {
      Nil
    }
  }
}

/**
 * DB components (for compile-time injection).
 */
trait DBComponents {
  def environment: Environment
  def configuration: Configuration
  def connectionPool: ConnectionPool
  def applicationLifecycle: ApplicationLifecycle

  lazy val dbApi: DBApi = new DBApiProvider(environment, configuration, connectionPool, applicationLifecycle).get
}

/**
 * Inject provider for DB implementation of DB API.
 */
@Singleton
class DBApiProvider @Inject() (environment: Environment, configuration: Configuration, connectionPool: ConnectionPool, lifecycle: ApplicationLifecycle) extends Provider[DBApi] {
  lazy val get: DBApi = {
    val config = configuration.getConfig("db").getOrElse(Configuration.empty)
    val db = new DefaultDBApi(config, connectionPool, environment.classLoader)
    lifecycle.addStopHook { () => Future.successful(db.shutdown()) }
    db.connect(logConnection = environment.mode != Mode.Test)
    db
  }
}

/**
 * Inject provider for named databases.
 */
class NamedDatabaseProvider(name: String) extends Provider[Database] {
  @Inject private var dbApi: DBApi = _
  lazy val get: Database = dbApi.database(name)
}
