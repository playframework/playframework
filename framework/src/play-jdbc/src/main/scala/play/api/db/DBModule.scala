/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */
package play.api.db

import javax.inject.{ Inject, Provider, Singleton }

import scala.concurrent.Future
import scala.util.control.NonFatal

import play.api.inject.{ ApplicationLifecycle, Binding, BindingKey, Module }
import play.api.{ Application, Configuration, Environment, Logger, Mode, Play }
import play.db.NamedDatabaseImpl

/**
 * DB runtime inject module.
 */
class DBModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val dbKey = configuration.underlying.getString("play.modules.db.config")
    val default = configuration.underlying.getString("play.modules.db.default")
    val dbs = configuration.getConfig(dbKey).getOrElse(Configuration.empty).subKeys
    Seq(
      bind[DBApi].toProvider[DBApiProvider]
    ) ++ namedDatabaseBindings(dbs) ++ defaultDatabaseBinding(default, dbs)
  }

  def bindNamed(name: String): BindingKey[Database] = {
    bind[Database].qualifiedWith(new NamedDatabaseImpl(name))
  }

  def namedDatabaseBindings(dbs: Set[String]): Seq[Binding[_]] = dbs.toSeq.map { db =>
    bindNamed(db).to(new NamedDatabaseProvider(db))
  }

  def defaultDatabaseBinding(default: String, dbs: Set[String]): Seq[Binding[_]] = {
    if (dbs.contains(default)) Seq(bind[Database].to(bindNamed(default))) else Nil
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
    val dbKey = configuration.underlying.getString("play.modules.db.config")
    val config = configuration.getConfig(dbKey).getOrElse(Configuration.empty)
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
