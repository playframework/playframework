/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import java.util.{ Date, UUID }
import java.sql.Connection

import scala.language.{ postfixOps, reflectiveCalls }
import scala.collection.TraversableOnce

import MayErr._

abstract class SqlRequestError
case class ColumnNotFound(columnName: String, possibilities: List[String]) extends SqlRequestError {
  override def toString = columnName + " not found, available columns : " + possibilities.map { p => p.dropWhile(c => c == '.') }
    .mkString(", ")
}

case class TypeDoesNotMatch(message: String) extends SqlRequestError
case class UnexpectedNullableFound(on: String) extends SqlRequestError
case class SqlMappingError(msg: String) extends SqlRequestError

abstract class Pk[+ID] {

  def toOption: Option[ID] = this match {
    case Id(x) => Some(x)
    case NotAssigned => None
  }

  def isDefined: Boolean = toOption.isDefined
  def get: ID = toOption.get
  def getOrElse[V >: ID](id: V): V = toOption.getOrElse(id)
  def map[B](f: ID => B) = toOption.map(f)
  def flatMap[B](f: ID => Option[B]) = toOption.flatMap(f)
  def foreach(f: ID => Unit) = toOption.foreach(f)

}

case class Id[ID](id: ID) extends Pk[ID] {
  override def toString() = id.toString
}

case object NotAssigned extends Pk[Nothing] {
  override def toString() = "NotAssigned"
}

case class MetaDataItem(column: ColumnName, nullable: Boolean, clazz: String)
case class ColumnName(qualified: String, alias: Option[String])

case class MetaData(ms: List[MetaDataItem]) {
  def get(columnName: String) = {
    val columnUpper = columnName.toUpperCase
    dictionary2.get(columnUpper).orElse(dictionary.get(columnUpper))
  }

  def getAliased(aliasName: String): Option[(ColumnName, Boolean, String)] =
    aliasedDictionary.get(aliasName.toUpperCase)

  private lazy val dictionary: Map[String, (ColumnName, Boolean, String)] =
    ms.map(m => (m.column.qualified.toUpperCase(), (m.column, m.nullable, m.clazz))).toMap

  private lazy val dictionary2: Map[String, (ColumnName, Boolean, String)] =
    ms.map(m => {
      val column = m.column.qualified.split('.').last;
      (column.toUpperCase(), (m.column, m.nullable, m.clazz))
    }).toMap

  private lazy val aliasedDictionary: Map[String, (ColumnName, Boolean, String)] = {
    ms.flatMap(m => {
      m.column.alias.map { a =>
        Map(a.toUpperCase() -> Tuple3(m.column, m.nullable, m.clazz))
      }.getOrElse(Map.empty)
    }).toMap
  }

  lazy val columnCount = ms.size

  lazy val availableColumns: List[String] =
    ms.flatMap(i => i.column.qualified :: i.column.alias.toList)

}

trait Row {
  val metaData: MetaData

  protected[anorm] val data: List[Any]

  /**
   * Returns row as list of column values.
   *
   * {{{
   * // Row first column is string "str", second one is integer 2
   * val l: List[Any] = row.asList
   * // l == List[Any]("str", 2)
   * }}}
   */
  lazy val asList: List[Any] = data.foldLeft[List[Any]](Nil) { (l, v) =>
    if (metaData.ms(l.size).nullable) l :+ Option(v) else l :+ v
  }

  /**
   * Returns row as dictionary of value per column name
   *
   * {{{
   * // Row column named 'A' is string "str", column named 'B' is integer 2
   * val m: Map[String, Any] = row.asMap
   * // l == Map[String, Any]("table.A" -> "str", "table.B" -> 2)
   * }}}
   */
  lazy val asMap: Map[String, Any] =
    data.foldLeft[Map[String, Any]](Map.empty) { (m, v) =>
      val d = metaData.ms(m.size)
      val k = d.column.qualified
      if (d.nullable) m + (k -> Option(v)) else m + (k -> v)
    }

  def get[A](a: String)(implicit c: Column[A]): MayErr[SqlRequestError, A] =
    SqlParser.get(a)(c)(this) match {
      case Success(a) => Right(a)
      case Error(e) => Left(e)
    }

  // TODO: Optimize
  private lazy val columnsDictionary: Map[String, Any] =
    metaData.ms.map(_.column.qualified.toUpperCase()).zip(data).toMap

  private lazy val aliasesDictionary: Map[String, Any] =
    metaData.ms.flatMap(_.column.alias.map(_.toUpperCase())).zip(data).toMap

  private[anorm] def get1(a: String): MayErr[SqlRequestError, Any] =
    for (
      meta <- metaData.get(a).
        toRight(ColumnNotFound(a, metaData.availableColumns));
      (column, nullable, clazz) = meta;
      result <- columnsDictionary.get(column.qualified.toUpperCase()).
        toRight(ColumnNotFound(column.qualified, metaData.availableColumns))
    ) yield result

  private[anorm] def getAliased(a: String): MayErr[SqlRequestError, Any] =
    for (
      meta <- metaData.getAliased(a).
        toRight(ColumnNotFound(a, metaData.availableColumns));
      (column, nullable, clazz) = meta;
      result <- column.alias.
        flatMap(a => aliasesDictionary.get(a.toUpperCase())).
        toRight(ColumnNotFound(column.alias.getOrElse(a),
          metaData.availableColumns))

    ) yield result

  def apply[B](a: String)(implicit c: Column[B]): B = get[B](a)(c).get

}

case class SqlRow(metaData: MetaData, data: List[Any]) extends Row {
  override def toString() = "Row(" + metaData.ms.zip(data).map(t => "'" + t._1.column + "':" + t._2 + " as " + t._1.clazz).mkString(", ") + ")"
}

object Useful {

  case class Var[T](var content: T)

  @deprecated(
    message = "Use [[scala.collection.immutable.Stream.dropWhile]] directly",
    since = "2.3.0")
  def drop[A](these: Var[Stream[A]], n: Int): Stream[A] = {
    var count = n
    while (!these.content.isEmpty && count > 0) {
      these.content = these.content.tail
      count -= 1
    }
    these.content
  }

  def unfold1[T, R](init: T)(f: T => Option[(R, T)]): (Stream[R], T) = f(init) match {
    case None => (Stream.Empty, init)
    case Some((r, v)) => (Stream.cons(r, unfold(v)(f)), v)
  }

  def unfold[T, R](init: T)(f: T => Option[(R, T)]): Stream[R] = f(init) match {
    case None => Stream.Empty
    case Some((r, v)) => Stream.cons(r, unfold(v)(f))
  }

}

import SqlParser._

/**
 * Prepared parameter value.
 */
trait ParameterValue {

  /**
   * Sets this value on given statement at specified index.
   *
   * @param s SQL Statement
   * @param index Parameter index
   */
  def set(s: java.sql.PreparedStatement, index: Int): Unit
}

/**
 * Value factory for parameter.
 *
 * {{{
 * val param = ParameterValue("str", setter)
 *
 * SQL("...").onParams(param)
 * }}}
 */
object ParameterValue {
  def apply[A](value: A, setter: ToStatement[A]) = new ParameterValue {
    def set(s: java.sql.PreparedStatement, i: Int) = setter.set(s, i, value)
  }
}

/** Applied named parameter. */
sealed case class NamedParameter(name: String, value: ParameterValue)

/** Companion object for applied named parameter. */
object NamedParameter {
  import scala.language.implicitConversions

  /**
   * Conversion to use tuple, with first element being name
   * of parameter as string.
   *
   * {{{
   * val p: Parameter = ("name" -> 1l)
   * }}}
   */
  implicit def string[V](t: (String, V))(implicit c: V => ParameterValue): NamedParameter = NamedParameter(t._1, c(t._2))

  /**
   * Conversion to use tuple,
   * with first element being symbolic name or parameter.
   *
   * {{{
   * val p: Parameter = ('name -> 1l)
   * }}}
   */
  implicit def symbol[V](t: (Symbol, V))(implicit c: V => ParameterValue): NamedParameter = NamedParameter(t._1.name, c(t._2))

}

case class SimpleSql[T](sql: SqlQuery, params: Seq[NamedParameter], defaultParser: RowParser[T]) extends Sql {

  /**
   * Returns query prepared with named parameters.
   *
   * {{{
   * import anorm.toParameterValue
   *
   * val baseSql = SQL("SELECT * FROM table WHERE id = {id}") // one named param
   * val preparedSql = baseSql.withParams("id" -> "value")
   * }}}
   */
  def on(args: NamedParameter*): SimpleSql[T] =
    copy(params = this.params ++ args)

  // Move down
  @annotation.tailrec
  private def zipParams(ns: Seq[String], vs: Seq[ParameterValue], ps: Seq[NamedParameter]): Seq[NamedParameter] = (ns.headOption, vs.headOption) match {
    case (Some(n), Some(v)) =>
      zipParams(ns.tail, vs.tail, ps :+ NamedParameter(n, v))
    case _ => ps
  }

  /**
   * Returns query prepared with indexed parameters.
   *
   * {{{
   * import anorm.toParameterValue
   *
   * val baseSql = SQL("SELECT * FROM table WHERE id = ?") // one indexed param
   * val preparedSql = baseSql.onParams("val_for_id")
   * }}}
   */
  def onParams(args: ParameterValue*): SimpleSql[T] =
    copy(params = this.params ++ zipParams(sql.argsInitialOrder, args, Nil))

  def list()(implicit connection: Connection): Seq[T] = as(defaultParser*)

  def single()(implicit connection: Connection): T = as(ResultSetParser.single(defaultParser))

  def singleOpt()(implicit connection: Connection): Option[T] = as(ResultSetParser.singleOpt(defaultParser))

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false) = {
    val s = if (getGeneratedKeys) connection.prepareStatement(sql.query, java.sql.Statement.RETURN_GENERATED_KEYS)
    else connection.prepareStatement(sql.query)

    sql.queryTimeout.foreach(timeout => s.setQueryTimeout(timeout))

    val argsMap = Map(params.map(p => p.name -> p.value): _*)
    sql.argsInitialOrder.map(argsMap)
      .zipWithIndex
      .map(_.swap)
      .foldLeft(s)((s, e) => { e._2.set(s, e._1 + 1); s })
  }

  /**
   * Prepares query with given row parser.
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * val res: Int = SQL("SELECT 1").using(SqlParser.scalar[Int]).single
   * // Equivalent to: SQL("SELECT 1").as(SqlParser.scalar[Int].single)
   * }}}
   */
  def using[U](p: RowParser[U]): SimpleSql[U] = // Deprecates with .as ?
    copy(sql, params, p)

  def map[A](f: T => A): SimpleSql[A] =
    copy(defaultParser = defaultParser.map(f))

  def withQueryTimeout(seconds: Option[Int]): SimpleSql[T] =
    copy(sql = sql.withQueryTimeout(seconds))
}

case class BatchSql(sql: SqlQuery, params: Seq[Seq[(String, ParameterValue)]]) {

  def addBatch(args: (String, ParameterValue)*): BatchSql = copy(params = (this.params) :+ args)
  def addBatchList(paramsMapList: TraversableOnce[Seq[(String, ParameterValue)]]): BatchSql = copy(params = (this.params) ++ paramsMapList)

  def addBatchParams(args: ParameterValue*): BatchSql =
    copy(params = (this.params) :+ sql.argsInitialOrder.zip(args))

  def addBatchParamsList(paramsSeqList: TraversableOnce[Seq[ParameterValue]]): BatchSql = copy(params = (this.params) ++ paramsSeqList.map(paramsSeq => sql.argsInitialOrder.zip(paramsSeq)))

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false) = {
    val statement = if (getGeneratedKeys) connection.prepareStatement(sql.query, java.sql.Statement.RETURN_GENERATED_KEYS)
    else connection.prepareStatement(sql.query)

    sql.queryTimeout.foreach(timeout => statement.setQueryTimeout(timeout))

    params.foldLeft(statement)((s, ps) => {
      val argsMap = Map(ps: _*)
      val result = sql.argsInitialOrder
        .map(argsMap)
        .zipWithIndex
        .map(_.swap)
        .foldLeft(s)((s, e) => { e._2.set(s, e._1 + 1); s })
      s.addBatch()
      result
    })
  }

  @deprecated(message = "Use [[getFilledStatement]]", since = "2.3.0")
  def filledStatement(implicit connection: Connection) = getFilledStatement(connection)

  def execute()(implicit connection: Connection): Array[Int] = getFilledStatement(connection).executeBatch()

  def withQueryTimeout(seconds: Option[Int]): BatchSql =
    copy(sql = sql.withQueryTimeout(seconds))
}

trait Sql {

  import SqlParser._
  import scala.util.control.Exception._

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false): java.sql.PreparedStatement

  @deprecated(message = "Use [[getFilledStatement]] or [[executeQuery]]", since = "2.3.0")
  def filledStatement(implicit connection: Connection) = getFilledStatement(connection)

  def apply()(implicit connection: Connection) = Sql.resultSetToStream(resultSet())

  def resultSet()(implicit connection: Connection) = (getFilledStatement(connection).executeQuery())

  import SqlParser._

  def as[T](parser: ResultSetParser[T])(implicit connection: Connection): T = Sql.as[T](parser, resultSet())

  def list[A](rowParser: RowParser[A])(implicit connection: Connection): Seq[A] = as(rowParser *)

  def single[A](rowParser: RowParser[A])(implicit connection: Connection): A = as(ResultSetParser.single(rowParser))

  def singleOpt[A](rowParser: RowParser[A])(implicit connection: Connection): Option[A] = as(ResultSetParser.singleOpt(rowParser))

  def parse[T](parser: ResultSetParser[T])(implicit connection: Connection): T = Sql.parse[T](parser, resultSet())

  def execute()(implicit connection: Connection): Boolean = getFilledStatement(connection).execute()

  def execute1(getGeneratedKeys: Boolean = false)(implicit connection: Connection): (java.sql.PreparedStatement, Int) = {
    val statement = getFilledStatement(connection, getGeneratedKeys)
    (statement, { statement.executeUpdate() })
  }

  def executeUpdate()(implicit connection: Connection): Int =
    getFilledStatement(connection).executeUpdate()

  def executeInsert[A](generatedKeysParser: ResultSetParser[A] = scalar[Long].singleOpt)(implicit connection: Connection): A = {
    Sql.as(generatedKeysParser, execute1(getGeneratedKeys = true)._1.getGeneratedKeys)
  }

  /**
   * Executes this SQL query, and returns its result.
   *
   * {{{
   * implicit val conn: Connection = openConnection
   * val res: SqlQueryResult =
   *   SQL("SELECT text_col FROM table WHERE id = {code}").
   *   on("code" -> code).executeQuery()
   * // Check execution context; e.g. res.statementWarning
   * val str = res as scalar[String].single // going with row parsing
   * }}}
   */
  def executeQuery()(implicit connection: Connection): SqlQueryResult =
    SqlQueryResult(resultSet())

}

case class SqlQuery(query: String, argsInitialOrder: List[String] = List.empty, queryTimeout: Option[Int] = None) extends Sql {

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false): java.sql.PreparedStatement =
    asSimple.getFilledStatement(connection, getGeneratedKeys)

  def withQueryTimeout(seconds: Option[Int]): SqlQuery =
    this.copy(queryTimeout = seconds)

  private def defaultParser: RowParser[Row] = RowParser(row => Success(row))

  def asSimple: SimpleSql[Row] = asSimple(defaultParser)

  /**
   * Prepares query as a simple one.
   * @param parser Row parser
   *
   * {{{
   * import anorm.{ SQL, SqlParser }
   *
   * SQL("SELECT 1").asSimple(SqlParser.scalar[Int])
   * }}}
   */
  def asSimple[T](parser: RowParser[T] = defaultParser): SimpleSql[T] =
    SimpleSql(this, Nil, parser)

  def asBatch[T]: BatchSql = BatchSql(this, Nil)
}

/**
 * A result from execution of an SQL query, row data and context
 * (e.g. statement warnings).
 *
 * @constructor create a result with a result set
 * @param resultSet Result set from executed query
 */
case class SqlQueryResult(resultSet: java.sql.ResultSet) {
  import SqlParser._

  /** Query statement already executed */
  val statement: java.sql.Statement = resultSet.getStatement

  /**
   * Returns statement warning if there is some for this result.
   *
   * {{{
   * val res = SQL("EXEC stored_proc {p}").on("p" -> paramVal).executeQuery()
   * res.statementWarning match {
   *   case Some(warning) =>
   *     warning.printStackTrace()
   *     None
   *
   *   case None =>
   *     // go on with row parsing ...
   *     res.as(scalar[String].singleOpt)
   * }
   * }}}
   */
  def statementWarning: Option[java.sql.SQLWarning] =
    Option(statement.getWarnings)

  def apply()(implicit connection: Connection) = Sql.resultSetToStream(resultSet)

  def as[T](parser: ResultSetParser[T])(implicit connection: Connection): T = Sql.as[T](parser, resultSet)

  def list[A](rowParser: RowParser[A])(implicit connection: Connection): Seq[A] = as(rowParser *)

  def single[A](rowParser: RowParser[A])(implicit connection: Connection): A = as(ResultSetParser.single(rowParser))

  def singleOpt[A](rowParser: RowParser[A])(implicit connection: Connection): Option[A] = as(ResultSetParser.singleOpt(rowParser))

  def parse[T](parser: ResultSetParser[T])(implicit connection: Connection): T = Sql.parse[T](parser, resultSet)
}

object Sql {

  def sql(inSql: String): SqlQuery = {
    val (sql, paramsNames) = SqlStatementParser.parse(inSql)
    SqlQuery(sql, paramsNames)
  }

  import java.sql._
  import java.sql.ResultSetMetaData._

  def metaData(rs: java.sql.ResultSet) = {
    val meta = rs.getMetaData()
    val nbColumns = meta.getColumnCount()
    MetaData(List.range(1, nbColumns + 1).map(i =>
      MetaDataItem(column = ColumnName({

        // HACK FOR POSTGRES - Fix in https://github.com/pgjdbc/pgjdbc/pull/107
        if (meta.getClass.getName.startsWith("org.postgresql.")) {
          meta.asInstanceOf[{ def getBaseTableName(i: Int): String }].getBaseTableName(i)
        } else {
          meta.getTableName(i)
        }

      } + "." + meta.getColumnName(i), alias = Option(meta.getColumnLabel(i))),
        nullable = meta.isNullable(i) == columnNullable,
        clazz = meta.getColumnClassName(i))))
  }

  def resultSetToStream(rs: java.sql.ResultSet): Stream[SqlRow] = {
    val rsMetaData = metaData(rs)
    val columns = List.range(1, rsMetaData.columnCount + 1)
    def data(rs: java.sql.ResultSet) = columns.map(nb => rs.getObject(nb))
    Useful.unfold(rs)(rs => if (!rs.next()) { rs.getStatement.close(); None } else Some((new SqlRow(rsMetaData, data(rs)), rs)))
  }

  def as[T](parser: ResultSetParser[T], rs: java.sql.ResultSet): T =
    parser(resultSetToStream(rs)) match {
      case Success(a) => a
      case Error(e) => sys.error(e.toString)
    }

  def parse[T](parser: ResultSetParser[T], rs: java.sql.ResultSet): T =
    parser(resultSetToStream(rs)) match {
      case Success(a) => a
      case Error(e) => sys.error(e.toString)
    }

}
