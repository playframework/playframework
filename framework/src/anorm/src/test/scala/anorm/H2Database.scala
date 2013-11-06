package anorm

import java.sql.{DriverManager, Connection}
import scala.util.Random

trait H2Database {

  def withConnection[R](block: Connection => R) = {
    val dbName = "test" + Random.alphanumeric.take(6).mkString("")

    Class.forName("org.h2.Driver")
    val connection = DriverManager.getConnection("jdbc:h2:mem:" + dbName, "sa", "")
    try {
      block(connection)
    } finally {
      connection.close()
    }
  }

  case class TestTable(id: Long, foo: String, bar: Int)

  /**
   * Create a simple test table for testing with.
   */
  def createTestTable()(implicit conn: Connection): Unit = createTable("test", "id bigint", "foo varchar", "bar int")

  def createTable(name: String, columns: String*)(implicit conn: Connection): Unit = {
    conn.createStatement().execute("create table " + name + " ( " + columns.mkString(", ") + ");")
  }

  def createAlias(name: String, code: String)(implicit conn: Connection): Unit = {
    conn.createStatement().execute("CREATE ALIAS %s AS $$%s$$;".format(name, code))
  }
}
