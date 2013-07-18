package anorm

import scala.language.{ postfixOps, reflectiveCalls }

import MayErr._
import java.util.Date
import collection.TraversableOnce

abstract class SqlRequestError
case class ColumnNotFound(columnName: String, possibilities: List[String]) extends SqlRequestError {
  override def toString = columnName + " not found, available columns : " + possibilities.map { p => p.dropWhile(c => c == '.') }
    .mkString(", ")
}

case class TypeDoesNotMatch(message: String) extends SqlRequestError
case class UnexpectedNullableFound(on: String) extends SqlRequestError
case object NoColumnsInReturnedResult extends SqlRequestError
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

trait Column[A] extends ((Any, MetaDataItem) => MayErr[SqlRequestError, A])

object Column {

  def apply[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = new Column[A] {

    def apply(value: Any, meta: MetaDataItem): MayErr[SqlRequestError, A] = transformer(value, meta)

  }

  def nonNull[A](transformer: ((Any, MetaDataItem) => MayErr[SqlRequestError, A])): Column[A] = Column[A] {
    case (value, meta @ MetaDataItem(qualified, _, _)) =>
      if (value != null) transformer(value, meta) else Left(UnexpectedNullableFound(qualified.toString))
  }

  implicit def rowToString: Column[String] = {
    Column.nonNull[String] { (value, meta) =>
      val MetaDataItem(qualified, nullable, clazz) = meta
      value match {
        case string: String => Right(string)
        case clob: java.sql.Clob => Right(clob.getSubString(1, clob.length.asInstanceOf[Int]))
        case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to String for column " + qualified))
      }
    }
  }

  implicit def rowToInt: Column[Int] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Int for column " + qualified))
    }
  }

  implicit def rowToDouble: Column[Double] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case d: Double => Right(d)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Double for column " + qualified))
    }
  }

  implicit def rowToShort: Column[Short] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case short: Short => Right(short)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Short for column " + qualified))
    }
  }

  implicit def rowToByte: Column[Byte] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case byte: Byte => Right(byte)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Byte for column " + qualified))
    }
  }

  implicit def rowToBoolean: Column[Boolean] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bool: Boolean => Right(bool)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Boolean for column " + qualified))
    }
  }

  implicit def rowToLong: Column[Long] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case int: Int => Right(int: Long)
      case long: Long => Right(long)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Long for column " + qualified))
    }
  }

  implicit def rowToBigInteger: Column[java.math.BigInteger] = Column.nonNull { (value, meta) =>
    import java.math.BigInteger
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: BigInteger => Right(bi)
      case int: Int => Right(BigInteger.valueOf(int))
      case long: Long => Right(BigInteger.valueOf(long))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to BigInteger for column " + qualified))
    }
  }

  implicit def rowToBigDecimal: Column[java.math.BigDecimal] = Column.nonNull { (value, meta) =>
    import java.math.BigDecimal
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case bi: java.math.BigDecimal => Right(bi)
      case double: Double => Right(new java.math.BigDecimal(double))
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to BigDecimal for column " + qualified))
    }
  }

  implicit def rowToDate: Column[Date] = Column.nonNull { (value, meta) =>
    val MetaDataItem(qualified, nullable, clazz) = meta
    value match {
      case date: Date => Right(date)
      case _ => Left(TypeDoesNotMatch("Cannot convert " + value + ":" + value.asInstanceOf[AnyRef].getClass + " to Date for column " + qualified))
    }
  }

  implicit def rowToPk[T](implicit c: Column[T]): Column[Pk[T]] = Column.nonNull { (value, meta) =>
    c(value, meta).map(Id(_))

  }

  implicit def rowToOption[T](implicit transformer: Column[T]): Column[Option[T]] = Column { (value, meta) =>
    if (value != null) transformer(value, meta).map(Some(_)) else (Right(None): MayErr[SqlRequestError, Option[T]])
  }

}

case class TupleFlattener[F](f: F)

trait PriorityOne {
  implicit def flattenerTo2[T1, T2]: TupleFlattener[(T1 ~ T2) => (T1, T2)] = TupleFlattener[(T1 ~ T2) => (T1, T2)] { case (t1 ~ t2) => (t1, t2) }
}

trait PriorityTwo extends PriorityOne {
  implicit def flattenerTo3[T1, T2, T3]: TupleFlattener[(T1 ~ T2 ~ T3) => (T1, T2, T3)] = TupleFlattener[(T1 ~ T2 ~ T3) => (T1, T2, T3)] { case (t1 ~ t2 ~ t3) => (t1, t2, t3) }
}

trait PriorityThree extends PriorityTwo {
  implicit def flattenerTo4[T1, T2, T3, T4]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4) => (T1, T2, T3, T4)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4) => (T1, T2, T3, T4)] { case (t1 ~ t2 ~ t3 ~ t4) => (t1, t2, t3, t4) }
}

trait PriorityFour extends PriorityThree {
  implicit def flattenerTo5[T1, T2, T3, T4, T5]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5) => (T1, T2, T3, T4, T5)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5) => (T1, T2, T3, T4, T5)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5) => (t1, t2, t3, t4, t5) }
}

trait PriorityFive extends PriorityFour {
  implicit def flattenerTo6[T1, T2, T3, T4, T5, T6]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6) => (T1, T2, T3, T4, T5, T6)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6) => (T1, T2, T3, T4, T5, T6)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6) => (t1, t2, t3, t4, t5, t6) }
}

trait PrioritySix extends PriorityFive {
  implicit def flattenerTo7[T1, T2, T3, T4, T5, T6, T7]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7) => (T1, T2, T3, T4, T5, T6, T7)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7) => (T1, T2, T3, T4, T5, T6, T7)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6 ~ t7) => (t1, t2, t3, t4, t5, t6, t7) }
}

trait PrioritySeven extends PrioritySix {
  implicit def flattenerTo8[T1, T2, T3, T4, T5, T6, T7, T8]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8) => (T1, T2, T3, T4, T5, T6, T7, T8)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8) => (T1, T2, T3, T4, T5, T6, T7, T8)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6 ~ t7 ~ t8) => (t1, t2, t3, t4, t5, t6, t7, t8) }
}

trait PriorityEight extends PrioritySeven {
  implicit def flattenerTo9[T1, T2, T3, T4, T5, T6, T7, T8, T9]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9) => (T1, T2, T3, T4, T5, T6, T7, T8, T9)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9) => (T1, T2, T3, T4, T5, T6, T7, T8, T9)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6 ~ t7 ~ t8 ~ t9) => (t1, t2, t3, t4, t5, t6, t7, t8, t9) }
}

trait PriorityNine extends PriorityEight {
  implicit def flattenerTo10[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6 ~ t7 ~ t8 ~ t9 ~ t10) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10) }
}

object TupleFlattener extends PriorityNine {
  implicit def flattenerTo11[T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11]: TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] = TupleFlattener[(T1 ~ T2 ~ T3 ~ T4 ~ T5 ~ T6 ~ T7 ~ T8 ~ T9 ~ T10 ~ T11) => (T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11)] { case (t1 ~ t2 ~ t3 ~ t4 ~ t5 ~ t6 ~ t7 ~ t8 ~ t9 ~ t10 ~ t11) => (t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11) }
}

object Row {
  def unapplySeq(row: Row): Option[List[Any]] = Some(row.asList)
}

case class MetaDataItem(column: ColumnName, nullable: Boolean, clazz: String)
case class ColumnName(qualified: String, alias: Option[String])

case class MetaData(ms: List[MetaDataItem]) {
  def get(columnName: String) = {
    val columnUpper = columnName.toUpperCase()
    dictionary2.get(columnUpper)
      .orElse(dictionary.get(columnUpper))
  }

  def getAliased(aliasName: String) = {
    val columnUpper = aliasName.toUpperCase()
    aliasedDictionary.get(columnUpper)
  }

  private lazy val dictionary: Map[String, (ColumnName, Boolean, String)] =
    ms.map(m => (m.column.qualified.toUpperCase(), (m.column, m.nullable, m.clazz))).toMap

  private lazy val dictionary2: Map[String, (ColumnName, Boolean, String)] = {
    ms.map(m => {
      val column = m.column.qualified.split('.').last;
      (column.toUpperCase(), (m.column, m.nullable, m.clazz))
    }).toMap
  }

  private lazy val aliasedDictionary: Map[String, (ColumnName, Boolean, String)] = {
    ms.flatMap(m => {
      m.column.alias.map { a =>
        Map(a.toUpperCase() -> (m.column, m.nullable, m.clazz))
      }.getOrElse(Map.empty)

    }).toMap
  }

  lazy val columnCount = ms.size

  lazy val availableColumns: List[String] = ms.flatMap(i => i.column.qualified :: i.column.alias.toList)

}

trait Row {

  val metaData: MetaData

  import scala.reflect.Manifest

  protected[anorm] val data: List[Any]

  lazy val asList = data.zip(metaData.ms.map(_.nullable)).map(i => if (i._2) Option(i._1) else i._1)

  lazy val asMap: scala.collection.Map[String, Any] = metaData.ms.map(_.column.qualified).zip(asList).toMap

  def get[A](a: String)(implicit c: Column[A]): MayErr[SqlRequestError, A] = SqlParser.get(a)(c)(this) match {
    case Success(a) => Right(a)
    case Error(e) => Left(e)
  }

  private def getType(t: String) = t match {
    case "long" => Class.forName("java.lang.Long")
    case "int" => Class.forName("java.lang.Integer")
    case "boolean" => Class.forName("java.lang.Boolean")
    case _ => Class.forName(t)
  }

  private lazy val ColumnsDictionary: Map[String, Any] = metaData.ms.map(_.column.qualified.toUpperCase()).zip(data).toMap
  private lazy val AliasesDictionary: Map[String, Any] = metaData.ms.flatMap(_.column.alias.map(_.toUpperCase())).zip(data).toMap
  private[anorm] def get1(a: String): MayErr[SqlRequestError, Any] = {
    for (
      meta <- metaData.get(a).toRight(ColumnNotFound(a, metaData.availableColumns));
      (column, nullable, clazz) = meta;
      result <- ColumnsDictionary.get(column.qualified.toUpperCase()).toRight(ColumnNotFound(column.qualified, metaData.availableColumns))
    ) yield result
  }

  private[anorm] def getAliased(a: String): MayErr[SqlRequestError, Any] = {
    for (
      meta <- metaData.getAliased(a).toRight(ColumnNotFound(a, metaData.availableColumns));
      (column, nullable, clazz) = meta;
      result <- column.alias.flatMap(a => AliasesDictionary.get(a.toUpperCase())).toRight(ColumnNotFound(column.alias.getOrElse(a), metaData.availableColumns))
    ) yield result
  }

  def apply[B](a: String)(implicit c: Column[B]): B = get[B](a)(c).get

}

case class MockRow(data: List[Any], metaData: MetaData) extends Row

case class SqlRow(metaData: MetaData, data: List[Any]) extends Row {
  override def toString() = "Row(" + metaData.ms.zip(data).map(t => "'" + t._1.column + "':" + t._2 + " as " + t._1.clazz).mkString(", ") + ")"
}

object Useful {

  case class Var[T](var content: T)

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

trait ToStatement[A] { def set(s: java.sql.PreparedStatement, index: Int, aValue: A): Unit }
object ToStatement {

  implicit def anyParameter[T] = new ToStatement[T] {
    private def setAny(index: Int, value: Any, stmt: java.sql.PreparedStatement): java.sql.PreparedStatement = {
      value match {
        case Some(bd: java.math.BigDecimal) => stmt.setBigDecimal(index, bd)
        case Some(o) => stmt.setObject(index, o)
        case None => stmt.setObject(index, null)
        case bd: java.math.BigDecimal => stmt.setBigDecimal(index, bd)
        case date: java.util.Date => stmt.setTimestamp(index, new java.sql.Timestamp(date.getTime()))
        case Id(id) => stmt.setObject(index, id)
        case NotAssigned => stmt.setObject(index, null)
        case o => stmt.setObject(index, o)
      }
      stmt
    }

    def set(s: java.sql.PreparedStatement, index: Int, aValue: T): Unit = setAny(index, aValue, s)
  }

  implicit val dateToStatement = new ToStatement[java.util.Date] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: java.util.Date): Unit = s.setTimestamp(index, new java.sql.Timestamp(aValue.getTime()))

  }

  implicit def optionToStatement[A](implicit ts: ToStatement[A]): ToStatement[Option[A]] = new ToStatement[Option[A]] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: Option[A]): Unit = {
      aValue match {
        case Some(o) => ts.set(s, index, o)
        case None => s.setObject(index, null)
      }
    }
  }

  implicit def pkToStatement[A](implicit ts: ToStatement[A]): ToStatement[Pk[A]] = new ToStatement[Pk[A]] {
    def set(s: java.sql.PreparedStatement, index: Int, aValue: Pk[A]): Unit =
      aValue match {
        case Id(id) => ts.set(s, index, id)
        case NotAssigned => s.setObject(index, null)
      }
  }

}

import SqlParser._
case class ParameterValue[A](aValue: A, statementSetter: ToStatement[A]) {
  def set(s: java.sql.PreparedStatement, index: Int) = statementSetter.set(s, index, aValue)
}

case class SimpleSql[T](sql: SqlQuery, params: Seq[(String, ParameterValue[_])], defaultParser: RowParser[T]) extends Sql {

  def on(args: (Any, ParameterValue[_])*): SimpleSql[T] = this.copy(params = (this.params) ++ args.map {
    case (s: Symbol, v) => (s.name, v)
    case (k, v) => (k.toString, v)
  })

  def onParams(args: ParameterValue[_]*): SimpleSql[T] = this.copy(params = (this.params) ++ sql.argsInitialOrder.zip(args))

  def list()(implicit connection: java.sql.Connection): Seq[T] = as(defaultParser*)

  def single()(implicit connection: java.sql.Connection): T = as(ResultSetParser.single(defaultParser))

  def singleOpt()(implicit connection: java.sql.Connection): Option[T] = as(ResultSetParser.singleOpt(defaultParser))

  //def first()(implicit connection: java.sql.Connection): Option[T] = parse((guard(acceptMatch("not at end", { case Right(_) => Unit })) ~> commit(defaultParser))?)

  def getFilledStatement(connection: java.sql.Connection, getGeneratedKeys: Boolean = false) = {
    val s = if (getGeneratedKeys) connection.prepareStatement(sql.query, java.sql.Statement.RETURN_GENERATED_KEYS)
    else connection.prepareStatement(sql.query)

    sql.queryTimeout.foreach(timeout => s.setQueryTimeout(timeout))

    val argsMap = Map(params: _*)
    sql.argsInitialOrder.map(argsMap)
      .zipWithIndex
      .map(_.swap)
      .foldLeft(s)((s, e) => { e._2.set(s, e._1 + 1); s })
  }

  def using[U](p: RowParser[U]): SimpleSql[U] = SimpleSql(sql, params, p)

  def withQueryTimeout(seconds: Option[Int]): SimpleSql[T] = this.copy(sql = sql.withQueryTimeout(seconds))
}

case class BatchSql(sql: SqlQuery, params: Seq[Seq[(String, ParameterValue[_])]]) {

  def addBatch(args: (String, ParameterValue[_])*): BatchSql = this.copy(params = (this.params) :+ args)
  def addBatchList(paramsMapList: TraversableOnce[Seq[(String, ParameterValue[_])]]): BatchSql = this.copy(params = (this.params) ++ paramsMapList)

  def addBatchParams(args: ParameterValue[_]*): BatchSql = this.copy(params = (this.params) :+ sql.argsInitialOrder.zip(args))
  def addBatchParamsList(paramsSeqList: TraversableOnce[Seq[ParameterValue[_]]]): BatchSql = this.copy(params = (this.params) ++ paramsSeqList.map(paramsSeq => sql.argsInitialOrder.zip(paramsSeq)))

  def getFilledStatement(connection: java.sql.Connection, getGeneratedKeys: Boolean = false) = {
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

  def filledStatement(implicit connection: java.sql.Connection) = getFilledStatement(connection)

  def execute()(implicit connection: java.sql.Connection): Array[Int] = getFilledStatement(connection).executeBatch()

  def withQueryTimeout(seconds: Option[Int]): BatchSql = this.copy(sql = sql.withQueryTimeout(seconds))
}

trait Sql {

  import SqlParser._
  import scala.util.control.Exception._

  def getFilledStatement(connection: java.sql.Connection, getGeneratedKeys: Boolean = false): java.sql.PreparedStatement

  def filledStatement(implicit connection: java.sql.Connection) = getFilledStatement(connection)

  def apply()(implicit connection: java.sql.Connection) = Sql.resultSetToStream(resultSet())

  def resultSet()(implicit connection: java.sql.Connection) = (getFilledStatement(connection).executeQuery())

  import SqlParser._

  def as[T](parser: ResultSetParser[T])(implicit connection: java.sql.Connection): T = Sql.as[T](parser, resultSet())

  def list[A](rowParser: RowParser[A])(implicit connection: java.sql.Connection): Seq[A] = as(rowParser *)

  def single[A](rowParser: RowParser[A])(implicit connection: java.sql.Connection): A = as(ResultSetParser.single(rowParser))

  def singleOpt[A](rowParser: RowParser[A])(implicit connection: java.sql.Connection): Option[A] = as(ResultSetParser.singleOpt(rowParser))

  def parse[T](parser: ResultSetParser[T])(implicit connection: java.sql.Connection): T = Sql.parse[T](parser, resultSet())

  def execute()(implicit connection: java.sql.Connection): Boolean = getFilledStatement(connection).execute()

  def execute1(getGeneratedKeys: Boolean = false)(implicit connection: java.sql.Connection): (java.sql.PreparedStatement, Int) = {
    val statement = getFilledStatement(connection, getGeneratedKeys)
    (statement, { statement.executeUpdate() })
  }

  def executeUpdate()(implicit connection: java.sql.Connection): Int =
    getFilledStatement(connection).executeUpdate()

  def executeInsert[A](generatedKeysParser: ResultSetParser[A] = scalar[Long].singleOpt)(implicit connection: java.sql.Connection): A = {
    Sql.as(generatedKeysParser, execute1(getGeneratedKeys = true)._1.getGeneratedKeys)
  }

}

case class SqlQuery(query: String, argsInitialOrder: List[String] = List.empty, queryTimeout: Option[Int] = None) extends Sql {

  def getFilledStatement(connection: java.sql.Connection, getGeneratedKeys: Boolean = false): java.sql.PreparedStatement =
    asSimple.getFilledStatement(connection, getGeneratedKeys)

  def withQueryTimeout(seconds: Option[Int]): SqlQuery = this.copy(queryTimeout = seconds)

  private def defaultParser: RowParser[Row] = RowParser(row => Success(row))

  def asSimple: SimpleSql[Row] = SimpleSql(this, Nil, defaultParser)

  def asSimple[T](parser: RowParser[T] = defaultParser): SimpleSql[T] = SimpleSql(this, Nil, parser)

  def asBatch[T]: BatchSql = BatchSql(this, Nil)
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

        // HACK FOR POSTGRES
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

  import SqlParser._

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
