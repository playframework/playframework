package play.api.db

import play.api.inject.{ Binding, Module }
import play.api.{ Configuration, Environment }

/**
 * DatabaseConfig runtime inject module.
 */
class DatabaseConfigModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[DatabaseConfig].toProvider[DatabaseConfigProvider]
    )
  }
}

trait DatabaseConfigComponent {
  def environment: Environment

  def configuration: Configuration

  lazy val databaseConfig: DatabaseConfig = new DatabaseConfigProvider(configuration, environment).get()
}
