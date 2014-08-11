/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package anorm

import java.util.{ Date, UUID }
import java.sql.{ Connection, PreparedStatement, ResultSet }

import scala.language.{ postfixOps, reflectiveCalls }
import scala.collection.TraversableOnce
import scala.runtime.{ ScalaRunTime, AbstractFunction3 }

import resource.{ managed, ManagedResource }

/** Error from processing SQL */
sealed trait SqlRequestError

case class ColumnNotFound(column: String, possibilities: List[String])
    extends SqlRequestError {

  override lazy val toString = s"$column not found, available columns : " +
    possibilities.map { _.dropWhile(_ == '.') }.mkString(", ")
}

case class TypeDoesNotMatch(message: String) extends SqlRequestError
case class UnexpectedNullableFound(on: String) extends SqlRequestError
case class SqlMappingError(msg: String) extends SqlRequestError

@deprecated(
  message = "Do not use directly, but consider [[Id]] or [[NotAssigned]].",
  since = "2.3.0")
trait Pk[+ID] extends NotNull {

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

/**
 * Untyped value wrapper.
 *
 * {{{
 * SQL("UPDATE t SET val = {o}").on('o -> anorm.Object(val))
 * }}}
 */
case class Object(value: Any)

case class MetaDataItem(column: ColumnName, nullable: Boolean, clazz: String)
case class ColumnName(qualified: String, alias: Option[String])

private[anorm] case class MetaData(ms: List[MetaDataItem]) {
  // TODO: Use MetaDataItem rather than (ColumnName, Boolean, String)?
  def get(columnName: String): Option[(ColumnName, Boolean, String)] = {
    val key = columnName.toUpperCase
    dictionary2.get(key).orElse(dictionary.get(key)).
      orElse(aliasedDictionary.get(key))
  }

  @deprecated(
    message = "No longer distinction between plain and aliased column",
    since = "2.3.3")
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

@deprecated(message = "Use directly Stream", since = "2.3.3")
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

  @deprecated(message = "Use directly Stream value", since = "2.3.2")
  def unfold1[T, R](init: T)(f: T => Option[(R, T)]): (Stream[R], T) = f(init) match {
    case None => (Stream.Empty, init)
    case Some((r, v)) => (Stream.cons(r, unfold(v)(f)), v)
  }

  def unfold[T, R](init: T)(f: T => Option[(R, T)]): Stream[R] = f(init) match {
    case None => Stream.Empty
    case Some((r, v)) => Stream.cons(r, unfold(v)(f))
  }
}

/**
 * Wrapper to use [[Seq]] as SQL parameter, with custom formatting.
 *
 * {{{
 * SQL("SELECT * FROM t WHERE %s").
 *   on(SeqParameter(Seq("a", "b"), " OR ", Some("cat = ")))
 * // Will execute as:
 * // SELECT * FROM t WHERE cat = 'a' OR cat = 'b'
 * }}}
 */
sealed trait SeqParameter[A] {
  def values: Seq[A]
  def separator: String
  def before: Option[String]
  def after: Option[String]
}

/** SeqParameter factory */
object SeqParameter {
  def apply[A](
    seq: Seq[A], sep: String = ", ",
    pre: String = "", post: String = ""): SeqParameter[A] =
    new SeqParameter[A] {
      val values = seq
      val separator = sep
      val before = Option(pre)
      val after = Option(post)
    }
}

/** Applied named parameter. */
sealed case class NamedParameter(name: String, value: ParameterValue) {
  lazy val tupled: (String, ParameterValue) = (name, value)
}

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

/** Simple/plain SQL. */
case class SimpleSql[T](sql: SqlQuery, params: Map[String, ParameterValue], defaultParser: RowParser[T]) extends Sql {

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
    copy(params = this.params ++ args.map(_.tupled))

  /**
   * Returns query prepared with parameters using initial order
   * of placeholder in statement.
   *
   * {{{
   * import anorm.toParameterValue
   *
   * val baseSql =
   *   SQL("SELECT * FROM table WHERE name = {name} AND lang = {lang}")
   *
   * val preparedSql = baseSql.onParams("1st", "2nd")
   * // 1st param = name, 2nd param = lang
   * }}}
   */
  def onParams(args: ParameterValue*): SimpleSql[T] =
    copy(params = this.params ++ Sql.zipParams(
      sql.argsInitialOrder, args, Map.empty))

  // TODO: Scaladoc as `as` equivalent
  def list()(implicit connection: Connection): Seq[T] = as(defaultParser.*)

  // TODO: Scaladoc as `as` equivalent
  def single()(implicit connection: Connection): T = as(defaultParser.single)

  // TODO: Scaladoc, add to specs as `as` equivalent
  def singleOpt()(implicit connection: Connection): Option[T] =
    as(defaultParser.singleOpt)

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false) = {
    val st: (String, Seq[(Int, ParameterValue)]) = Sql.prepareQuery(
      sql.query, 0, sql.argsInitialOrder.map(params), Nil)

    val stmt = if (getGeneratedKeys) connection.prepareStatement(st._1, java.sql.Statement.RETURN_GENERATED_KEYS) else connection.prepareStatement(st._1)

    sql.queryTimeout.foreach(stmt.setQueryTimeout(_))

    st._2 foreach { p =>
      val (i, v) = p
      v.set(stmt, i + 1)
    }

    stmt
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
  def using[U](p: RowParser[U]): SimpleSql[U] = copy(sql, params, p)
  // Deprecates with .as ?

  def map[A](f: T => A): SimpleSql[A] =
    copy(defaultParser = defaultParser.map(f))

  def withQueryTimeout(seconds: Option[Int]): SimpleSql[T] =
    copy(sql = sql.withQueryTimeout(seconds))

}

@deprecated(message = "Will be made private", since = "2.3.6")
sealed trait Sql {
  // TODO: ManagedResource[PreparedStatement]?
  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false): PreparedStatement

  @deprecated(message = "Use [[getFilledStatement]] or [[executeQuery]]", since = "2.3.0")
  def filledStatement(implicit connection: Connection) = getFilledStatement(connection)

  /**
   * Executes this SQL statement as query, returns result as Row stream.
   */
  @deprecated(message =
    "Use [[fold]] instead, which manages resources and memory", "2.4")
  def apply()(implicit connection: Connection): Stream[Row] =
    Sql.fold(resultSet())(Stream.empty[Row])((s, r) => (s :+ r) -> true).
      acquireAndGet(identity)

  /**
   * Aggregates over the whole stream of row using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator
   * @return Either list of failures at left, or aggregated value
   * @see #foldWhile
   */
  def fold[T](z: => T)(op: (T, Row) => T)(implicit connection: Connection): Either[List[Throwable], T] =
    Sql.fold(resultSet())(z) { (t, r) => op(t, r) -> true } acquireFor identity

  /**
   * Aggregates over part of or the while row stream, using the specified operator.
   *
   * @param z the start value
   * @param op Aggregate operator. Returns aggregated value along with true if aggregation must process next value, or false to stop with current value.
   * @return Either list of failures at left, or aggregated value
   */
  def foldWhile[T](z: => T)(op: (T, Row) => (T, Boolean))(implicit connection: Connection): Either[List[Throwable], T] =
    Sql.fold(resultSet())(z) { (t, r) => op(t, r) } acquireFor identity

  /**
   * Executes this statement as query (see [[executeQuery]]) and returns result.
   */
  private[anorm] def resultSet()(implicit connection: Connection): ManagedResource[ResultSet] = managed(getFilledStatement(connection)).
    flatMap(stmt => managed(stmt.executeQuery()))

  /**
   * Executes this statement as query and convert result as `T`, using parser.
   */
  def as[T](parser: ResultSetParser[T])(implicit connection: Connection): T =
    Sql.as(parser, resultSet())

  @deprecated(
    message = "Use [[as]] with rowParser.*",
    since = "2.3.0")
  def list[A](rowParser: RowParser[A])(implicit connection: Connection): Seq[A] = as(rowParser.*)

  @deprecated(
    message = "Use [[as]] with rowParser.single",
    since = "2.3.0")
  def single[T](rowParser: RowParser[T])(implicit connection: Connection): T = as(rowParser.single)

  @deprecated(
    message = "Use [[as]] with rowParser.singleOpt",
    since = "2.3.0")
  def singleOpt[T](rowParser: RowParser[T])(implicit connection: Connection): Option[T] = as(rowParser.singleOpt)

  @deprecated(message = "Use [[as]]", since = "2.3.0")
  def parse[T](parser: ResultSetParser[T])(implicit connection: Connection): T = as(parser)

  /**
   * Executes this SQL statement.
   * @return true if resultset was returned from execution
   * (statement is query), or false if it executed update.
   *
   * {{{
   * val res: Boolean =
   *   SQL"""INSERT INTO Test(a, b) VALUES(${"A"}, ${"B"}""".execute()
   * }}}
   */
  def execute()(implicit connection: Connection): Boolean =
    managed(getFilledStatement(connection)).acquireAndGet(_.execute())

  @deprecated(message = "Will be made private, use [[executeUpdate]] or [[executeInsert]]", since = "2.3.2")
  def execute1(getGeneratedKeys: Boolean = false)(implicit connection: Connection): (PreparedStatement, Int) = { // TODO: managed
    val statement = getFilledStatement(connection, getGeneratedKeys)
    (statement, statement.executeUpdate())
  }

  /**
   * Executes this SQL as an update statement.
   * @return Count of updated row(s)
   */
  @throws[java.sql.SQLException]("If statement is query not update")
  def executeUpdate()(implicit connection: Connection): Int =
    getFilledStatement(connection).executeUpdate()

  /**
   * Executes this SQL as an insert statement.
   *
   * @param generatedKeysParser Parser for generated key (default: scalar long)
   * @return Parsed generated keys
   *
   * {{{
   * import anorm.SqlParser.scalar
   *
   * val keys1 = SQL("INSERT INTO Test(x) VALUES ({x})").
   *   on("x" -> "y").executeInsert()
   *
   * val keys2 = SQL("INSERT INTO Test(x) VALUES ({x})").
   *   on("x" -> "y").executeInsert(scalar[String].singleOpt)
   * // ... generated string key
   * }}}
   */
  def executeInsert[A](generatedKeysParser: ResultSetParser[A] = SqlParser.scalar[Long].singleOpt)(implicit connection: Connection): A =
    Sql.as(generatedKeysParser, managed(getFilledStatement(connection, true)).
      flatMap { stmt =>
        stmt.executeUpdate()
        managed(stmt.getGeneratedKeys)
      })

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
    SqlQueryResult1(resultSet())

}

/**
 * Manually implemented so that we can deprecate all the case class methods
 */
object SqlQuery extends AbstractFunction3[String, List[String], Option[Int], SqlQuery] {
  @deprecated("Do not use the SqlQuery constructor directly because it does not parse the query. Use SQL(...) instead.", "2.3.2")
  def apply(query: String, argsInitialOrder: List[String] = List.empty, queryTimeout: Option[Int] = None) =
    new SqlQuery(query, argsInitialOrder, queryTimeout)

  @deprecated("SqlQuery will become a trait in 2.4.0", "2.3.2")
  def unapply(value: SqlQuery): Option[(String, List[String], Option[Int])] =
    Some((value.query, value.argsInitialOrder, value.queryTimeout))
}

/** Initial SQL query, without parameter values. */
class SqlQuery @deprecated("Do not use the SqlQuery constructor directly because it does not parse the query. Use SQL(...) instead.", "2.3.2") (val query: String, val argsInitialOrder: List[String] = List.empty, val queryTimeout: Option[Int] = None) extends Sql with Product with Serializable {

  def getFilledStatement(connection: Connection, getGeneratedKeys: Boolean = false): PreparedStatement = asSimple.getFilledStatement(connection, getGeneratedKeys)

  def withQueryTimeout(seconds: Option[Int]): SqlQuery =
    copy(queryTimeout = seconds)

  private def defaultParser: RowParser[Row] = RowParser(row => Success(row))

  private[anorm] def asSimple: SimpleSql[Row] = asSimple(defaultParser)

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
    SimpleSql(this, Map.empty, parser)

  def asBatch[T]: BatchSql = BatchSql(this, Nil)

  // Everything provided below just so we could deprecated the SqlQuery.apply method...
  // https://issues.scala-lang.org/browse/SI-8685

  @deprecated("SqlQuery will become a trait in 2.4.0", "2.3.2")
  def copy(query: String = this.query,
    argsInitialOrder: List[String] = this.argsInitialOrder,
    queryTimeout: Option[Int] = this.queryTimeout) = new SqlQuery(query, argsInitialOrder, queryTimeout)

  def productElement(n: Int) = n match {
    case 0 => this.query
    case 1 => this.argsInitialOrder
    case 2 => this.queryTimeout
    case _ => throw new IndexOutOfBoundsException(n.toString)
  }

  def productArity = 3

  def canEqual(that: Any) = that.isInstanceOf[SqlQuery]

  override def productPrefix = classOf[SqlQuery].getSimpleName

  override def hashCode() = ScalaRunTime._hashCode(this)

  override def equals(obj: scala.Any) = ScalaRunTime._equals(this, obj)

  override def toString = ScalaRunTime._toString(this)
}

object Sql { // TODO: Rename to SQL
  import java.sql.ResultSetMetaData

  private[anorm] def metaData(rs: ResultSet): MetaData = {
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
        nullable = meta.isNullable(i) == ResultSetMetaData.columnNullable,
        clazz = meta.getColumnClassName(i))))
  }

  private[anorm] def as[T](parser: ResultSetParser[T], rs: ManagedResource[ResultSet])(implicit connection: Connection): T =
    parser(fold(rs)(Stream.empty[Row])((s, r) => (s :+ r) -> true).
      acquireAndGet(identity)) match {
      case Success(a) => a
      case Error(e) => sys.error(e.toString)
    }

  /**
   * @param f Aggregate operator. Returns aggregated value along with true if aggregation must process next value, or false to stop with current value.
   */
  private[anorm] def fold[T](res: ManagedResource[ResultSet])(initial: => T)(f: (T, Row) => (T, Boolean)): ManagedResource[T] = res map { rs =>
    val rsMetaData: MetaData = metaData(rs)
    val columns: List[Int] = List.range(1, rsMetaData.columnCount + 1)
    @inline def data(rs: ResultSet) = columns.map(rs.getObject(_))

    @annotation.tailrec
    def go(r: ResultSet, s: T): T =
      if (r.next()) {
        val (v, cont) = f(s, new SqlRow(rsMetaData, data(rs)))
        if (cont) go(r, v) else v
      } else s

    go(rs, initial)
  }

  private case class SqlRow(metaData: MetaData, data: List[Any]) extends Row {
    override lazy val toString = "Row(" + metaData.ms.zip(data).map(t => s"'${t._1.column}': ${t._2} as ${t._1.clazz}").mkString(", ") + ")"
  }

  @annotation.tailrec
  private[anorm] def zipParams(ns: Seq[String], vs: Seq[ParameterValue], ps: Map[String, ParameterValue]): Map[String, ParameterValue] = (ns.headOption, vs.headOption) match {
    case (Some(n), Some(v)) =>
      zipParams(ns.tail, vs.tail, ps + (n -> v))
    case _ => ps
  }

  /**
   * Rewrites next format placeholder (%s) in statement, with fragment using
   * [[java.sql.PreparedStatement]] syntax (with one or more '?').
   *
   * @param statement SQL statement (with %s placeholders)
   * @param frag Statement fragment
   * @return Some rewrited statement, or None if there no available placeholder
   *
   * {{{
   * Sql.rewrite("SELECT * FROM Test WHERE cat IN (%s)", "?, ?")
   * // Some("SELECT * FROM Test WHERE cat IN (?, ?)")
   * }}}
   */
  private[anorm] def rewrite(stmt: String, frag: String): Option[String] = {
    val idx = stmt.indexOf("%s")

    if (idx == -1) None
    else {
      val parts = stmt.splitAt(idx)
      Some(parts._1 + frag + parts._2.drop(2))
    }
  }

  @annotation.tailrec
  private[anorm] def prepareQuery(sql: String, i: Int, ps: Seq[ParameterValue], vs: Seq[(Int, ParameterValue)]): (String, Seq[(Int, ParameterValue)]) = {
    ps.headOption match {
      case Some(p) =>
        val st: (String, Int) = p.toSql(sql, i)
        prepareQuery(st._1, st._2, ps.tail, vs :+ (i -> p))
      case _ => (sql, vs)
    }
  }
}
