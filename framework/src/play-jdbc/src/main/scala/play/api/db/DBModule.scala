/*
 * Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
 */

package play.api.db

import com.typesafe.config.Config
import javax.inject.{ Inject, Provider, Singleton }
import play.api._
import play.api.inject._
import play.db.NamedDatabaseImpl

import scala.concurrent.Future
import scala.util.Try

/**
 * DB runtime inject module.
 */
final class DBModule extends SimpleModule((environment, configuration) => {
  def bindNamed(name: String): BindingKey[Database] = {
    bind[Database].qualifiedWith(new NamedDatabaseImpl(name))
  }

  def namedDatabaseBindings(dbs: Set[String]): Seq[Binding[_]] = dbs.toSeq.map { db =>
    bindNamed(db).to(new NamedDatabaseProvider(db))
  }

  def defaultDatabaseBinding(default: String, dbs: Set[String]): Seq[Binding[_]] = {
    if (dbs.contains(default)) Seq(bind[Database].to(bindNamed(default))) else Nil
  }

  val dbKey = configuration.underlying.getString("play.db.config")
  val default = configuration.underlying.getString("play.db.default")
  val dbs = configuration.getOptional[Configuration](dbKey).getOrElse(Configuration.empty).subKeys
  Seq(
    bind[DBApi].toProvider[DBApiProvider]
  ) ++ namedDatabaseBindings(dbs) ++ defaultDatabaseBinding(default, dbs)
})

/**
 * DB components (for compile-time injection).
 */
trait DBComponents {
  def environment: Environment
  def configuration: Configuration
  def connectionPool: ConnectionPool
  def applicationLifecycle: ApplicationLifecycle

  lazy val dbApi: DBApi = new DBApiProvider(environment, configuration, connectionPool, applicationLifecycle, None).get
}

/**
 * Inject provider for DB implementation of DB API.
 */
@Singleton
class DBApiProvider(
    environment: Environment,
    configuration: Configuration,
    defaultConnectionPool: ConnectionPool,
    lifecycle: ApplicationLifecycle,
    maybeInjector: Option[Injector]
) extends Provider[DBApi] {

  @Inject
  def this(
    environment: Environment,
    configuration: Configuration,
    defaultConnectionPool: ConnectionPool,
    lifecycle: ApplicationLifecycle,
    injector: Injector = NewInstanceInjector
  ) = {
    this(environment, configuration, defaultConnectionPool, lifecycle, Option(injector))
  }

  lazy val get: DBApi = {
    val config = configuration.underlying
    val dbKey = config.getString("play.db.config")
    val pool = maybeInjector
      .map(injector => ConnectionPool.fromConfig(config.getString("play.db.pool"), injector, environment, defaultConnectionPool))
      .getOrElse(ConnectionPool.fromConfig(config.getString("play.db.pool"), environment, defaultConnectionPool))
    val configs = if (config.hasPath(dbKey)) {
      Configuration(config).getPrototypedMap(dbKey, "play.db.prototype").mapValues(_.underlying)
    } else Map.empty[String, Config]
    val db = new DefaultDBApi(configs, pool, environment, maybeInjector.getOrElse(NewInstanceInjector))
    lifecycle.addStopHook { () => Future.fromTry(Try(db.shutdown())) }
    db.initialize(logInitialization = environment.mode != Mode.Test)
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
