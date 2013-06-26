
/**
 * Anorm API
 *
 * Use the SQL method to start an SQL query
 *
 * {{{
 * import anorm._
 *
 * SQL("Select 1")
 * }}}
 */
package object anorm {
  import scala.language.implicitConversions

  implicit def sqlToSimple(sql: SqlQuery): SimpleSql[Row] = sql.asSimple
  implicit def sqlToBatch(sql: SqlQuery): BatchSql = sql.asBatch

  implicit def implicitID[ID](id: Id[ID] with NotNull): ID = id.id

  implicit def toParameterValue[A](a: A)(implicit p: ToStatement[A]): ParameterValue[A] =
    ParameterValue(a, p)

  def SQL(stmt: String) = Sql.sql(stmt)

}