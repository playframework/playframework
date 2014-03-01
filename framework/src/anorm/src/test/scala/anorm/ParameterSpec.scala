package anorm

import java.lang.{ Boolean => JBool }

import java.sql.SQLFeatureNotSupportedException

import acolyte.{
  DefinedParameter => DParam,
  ParameterMetaData,
  UpdateExecution
}
import acolyte.Acolyte.{ connection, handleStatement }
import acolyte.Implicits._

object ParameterSpec extends org.specs2.mutable.Specification {
  "Parameter" title

  val jbi1 = new java.math.BigInteger("1234")
  val sbi1 = BigInt(jbi1)
  val jbd1 = new java.math.BigDecimal(1.234d)
  val sbd1 = BigDecimal(jbd1)
  val date = new java.util.Date()
  val timestamp = {
    val ts = new java.sql.Timestamp(123l)
    ts.setNanos(123456789)
    ts
  }
  val SqlStr = ParameterMetaData.Str
  val SqlBool = ParameterMetaData.Bool
  val SqlInt = ParameterMetaData.Int
  val SqlByte = ParameterMetaData.Byte
  val SqlShort = ParameterMetaData.Short
  val SqlLong = ParameterMetaData.Long
  val SqlFloat = ParameterMetaData.Float(1.23f)
  val SqlDouble = ParameterMetaData.Double(23.456d)
  val SqlTimestamp = ParameterMetaData.Timestamp
  val SqlInt1 = ParameterMetaData.Numeric(new java.math.BigDecimal(jbi1))
  val SqlDec1 = ParameterMetaData.Numeric(jbd1)

  def withConnection[A](ps: (String, String)*)(f: java.sql.Connection => A): A = f(connection(handleStatement withUpdateHandler {
    case UpdateExecution("set-str ?",
      DParam("string", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-char ?",
      DParam("x", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-null-char ?",
      DParam(null, t) :: Nil) if (t.sqlTypeName == "CHAR") => 1 /* case ok */
    case UpdateExecution("set-false ?",
      DParam(false, SqlBool) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-true ?",
      DParam(true, SqlBool) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-null-bool ?",
      DParam(null, SqlBool) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-int ?",
      DParam(2, SqlInt) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-short ?",
      DParam(3, SqlShort) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-byte ?",
      DParam(4, SqlByte) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-long ?",
      DParam(5l, SqlLong) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-float ?",
      DParam(1.23f, SqlFloat) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-double ?",
      DParam(23.456d, SqlDouble) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-jbi ?",
      DParam(jbi1, SqlInt1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-sbi ?",
      DParam(sbi1, SqlInt1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-jbd ?",
      DParam(jbd1, SqlDec1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-sbd ?",
      DParam(sbd1, SqlDec1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-date ?",
      DParam(date, SqlTimestamp) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-timestamp ?",
      DParam(t: java.sql.Timestamp, SqlTimestamp) :: Nil) if t.getNanos == 123456789 => 1 /* case ok */
    case UpdateExecution("set-s-jbd ?, ?",
      DParam("string", SqlStr) :: DParam(jbd1, SqlDec1) :: Nil) => 1 /* ok */
    case UpdateExecution("set-s-sbd ?, ?",
      DParam("string", SqlStr) :: DParam(sbd1, SqlDec1) :: Nil) => 1 /* ok */
    case UpdateExecution("reorder-s-jbd ?, ?",
      DParam(jbd1, SqlDec1) :: DParam("string", SqlStr) :: Nil) => 1 /* ok */
    case UpdateExecution("set-some-str ?",
      DParam("string", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-str-opt ?",
      DParam("some_str", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-some-jbd ?",
      DParam(jbd1, SqlDec1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-some-sbd ?",
      DParam(sbd1, SqlDec1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-none ?", DParam(null, _) :: Nil)      => 1 /* ok */
    case UpdateExecution("set-empty-opt ?", DParam(null, _) :: Nil) => 1 /*ok*/
    case UpdateExecution("no-param-placeholder", Nil)               => 1 /* ok */
    case UpdateExecution("no-snd-placeholder ?",
      DParam("first", SqlStr) :: Nil) => 1 /* ok */
    case UpdateExecution("set-old ?", DParam(2l, SqlLong) :: Nil) => 1 // ok
    case UpdateExecution("set-id-str ?",
      DParam("str", SqlStr) :: Nil) => 1 /* ok */
    case UpdateExecution("set-not-assigned ?",
      DParam(null, _) :: Nil) => 1 /* ok */
    case UpdateExecution("set-seq ?, ?, ?",
      DParam("a", _) :: DParam("b", _) :: DParam("c", _) :: Nil) => 1 /* ok */
    case UpdateExecution("set-seqp cat = ? OR cat = ? OR cat = ?",
      DParam(1.2f, _) :: DParam(23.4f, _) :: DParam(5.6f, _) :: Nil) => 1 // ok
  }, ps: _*))

  "Named parameters" should {
    shapeless.test.illTyped {
      """("str".asInstanceOf[Any] -> 2) : NamedParameter"""
    }

    shapeless.test.illTyped {
      """"str".asInstanceOf[Any] : ParameterValue"""
    }

    "be one string with string name" in withConnection() { implicit c =>
      SQL("set-str {a}").on("a" -> "string").
        aka("query") must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-str %s", List("a"), _), ps, _) if (ps contains "a") =>

            // execute = false: update ok but returns no resultset
            // see java.sql.PreparedStatement#execute
            q.execute() aka "execution" must beFalse
        }
    }

    "be one string with symbol name" in withConnection() { implicit c =>
      SQL("set-str {b}").on('b -> "string").
        aka("query") must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-str %s", List("b"), _), ps, _) if (ps contains "b") =>
            q.execute() aka "execution" must beFalse
        }
    }

    "be one string with string interpolation" in withConnection() {
      implicit c =>
        SQL"""set-str ${"string"}""".
          aka("query") must beLike {
            case q @ SimpleSql( // check accross construction
              SqlQuery("set-str %s", List("_0"), _), ps, _) if (
              ps contains "_0") => q.execute() aka "execution" must beFalse
          }
    }

    "be character 'x'" in withConnection() { implicit c =>
      (SQL("set-char {b}").on('b -> new java.lang.Character('x')).
        aka("query") must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-char %s", List("b"), _), ps, _) if (
            ps contains "b") => q.execute() aka "execution" must beFalse
        }).and(SQL("set-char {b}").on('b -> Character.valueOf('x')).
          execute() aka "execution" must beFalse)
    }

    "be null character" in withConnection() { implicit c =>
      SQL("set-null-char {p}").on("p" -> null.asInstanceOf[Character]).
        execute() must beFalse
    }

    "be boolean true" in withConnection() { implicit c =>
      (SQL("set-true {p}").on("p" -> true).execute() must beFalse).
        and(SQL("set-true {p}").on("p" -> JBool.TRUE).execute() must beFalse)
    }

    "be boolean false" in withConnection() { implicit c =>
      (SQL("set-false {p}").on("p" -> false).execute() must beFalse).
        and(SQL("set-false {p}").on("p" -> JBool.FALSE).execute() must beFalse)
    }

    "be boolean NULL" in withConnection() { implicit c =>
      SQL("set-null-bool {p}").on('p -> null.asInstanceOf[JBool]).execute().
        aka("execution") must beFalse
    }

    "be int" in withConnection() { implicit c =>
      (SQL("set-int {p}").on("p" -> 2).execute() must beFalse).
        and(SQL("set-int {p}").on("p" -> new Integer(2)).execute() must beFalse)
    }

    "be short" in withConnection() { implicit c =>
      (SQL("set-short {p}").on("p" -> 3.toShort).execute() must beFalse).
        and(SQL("set-short {p}").on(
          "p" -> new java.lang.Short("3")).execute() must beFalse)
    }

    "be byte" in withConnection() { implicit c =>
      (SQL("set-byte {p}").on("p" -> 4.toByte).execute() must beFalse).
        and(SQL("set-byte {p}").on(
          "p" -> new java.lang.Byte("4")).execute() must beFalse)
    }

    "be long" in withConnection() { implicit c =>
      (SQL("set-long {p}").on("p" -> 5l).execute() must beFalse).
        and(SQL("set-long {p}").on(
          "p" -> new java.lang.Long(5)).execute() must beFalse)
    }

    "be float" in withConnection() { implicit c =>
      (SQL("set-float {p}").on("p" -> 1.23f).execute() must beFalse).
        and(SQL("set-float {p}").on(
          "p" -> new java.lang.Float(1.23f)).execute() must beFalse)
    }

    "be double" in withConnection() { implicit c =>
      (SQL("set-double {p}").on("p" -> 23.456d).execute() must beFalse).
        and(SQL("set-double {p}").on(
          "p" -> new java.lang.Double(23.456d)).execute() must beFalse)
    }

    "be one Java big integer" in withConnection() { implicit c =>
      SQL("set-jbi {p}").on("p" -> jbi1).execute() must beFalse
    }

    "be one Scala big integer" in withConnection() { implicit c =>
      SQL("set-sbi {p}").on("p" -> sbi1).execute() must beFalse
    }

    "be one Java big decimal" in withConnection() { implicit c =>
      SQL("set-jbd {p}").on("p" -> jbd1).execute() must beFalse
    }

    "be one Scala big decimal" in withConnection() { implicit c =>
      SQL("set-sbd {p}").on("p" -> sbd1).execute() must beFalse
    }

    "be one date" in withConnection() { implicit c =>
      SQL("set-date {p}").on("p" -> date).execute() must beFalse
    }

    "be one timestamp" in withConnection() { implicit c =>
      SQL("set-timestamp {p}").on("p" -> timestamp).execute() must beFalse
    }

    "be Id of string" in withConnection() { implicit c =>
      SQL("set-id-str {p}").on("p" -> Id("str")).execute() must beFalse
    }

    "be not assigned" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>

        SQL("set-not-assigned {p}").on("p" -> NotAssigned).
          execute() must beFalse
      }

    "be multiple (string, Java big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-jbd {a}, {b}").on("a" -> "string", "b" -> jbd1).
          aka("query") must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-jbd %s, %s", List("a", "b"), _),
              ps, _) if (ps.contains("a") && ps.contains("b")) =>
              q.execute() aka "execution" must beFalse

          }
    }

    "be multiple (string, Scala big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-sbd {a}, {b}").on("a" -> "string", "b" -> sbd1).
          execute() aka "execution" must beFalse
    }

    "be multiple (string, Scala big decimal) with string interpolation" in withConnection() {
      implicit c =>
        SQL"""set-s-sbd ${"string"}, $sbd1""".
          execute() aka "execution" must beFalse
    }

    "be reordered" in withConnection() { implicit c =>
      SQL("reorder-s-jbd {b}, {a}").on("a" -> "string", "b" -> jbd1).
        aka("query") must beLike {
          case q @ SimpleSql(
            SqlQuery("reorder-s-jbd %s, %s", List("b", "a"), _),
            ps, _) if (ps.contains("a") && ps.contains("b")) =>
            q.execute() aka "execution" must beFalse

        }
    }

    "be defined string option as Some[String]" in withConnection() {
      implicit c =>
        SQL("set-some-str {p}").on("p" -> Some("string")).execute().
          aka("execution") must beFalse
    }

    "be defined string option as Option[String]" in withConnection() {
      implicit c =>
        SQL("set-str-opt {p}").on("p" -> Option("some_str")).execute().
          aka("execution") must beFalse
    }

    "be defined Java big decimal option" in withConnection() { implicit c =>
      SQL("set-some-jbd {p}").on("p" -> Some(jbd1)).
        execute() aka "execution" must beFalse

    }

    "be defined Scala big decimal option" in withConnection() { implicit c =>
      SQL("set-some-sbd {p}").on("p" -> Some(sbd1)).
        execute() aka "execution" must beFalse

    }

    "not be set if placeholder not found in SQL" in withConnection() {
      implicit c =>
        SQL("no-param-placeholder").on("p" -> "not set").execute().
          aka("execution") must beFalse

    }

    "be partially set if matching placeholder is missing for second one" in {
      withConnection() { implicit c =>
        SQL("no-snd-placeholder {a}")
          .on("a" -> "first", "b" -> "second").execute() must beFalse

      }
    }

    "set null parameter from None" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        /*
       http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
       -> Note: Not all databases allow for a non-typed Null to be sent to the backend. For maximum portability, the setNull or the setObject(int parameterIndex, Object x, int sqlType) method should be used instead of setObject(int parameterIndex, Object x).
       -> Note: This method throws an exception if there is an ambiguity, for example, if the object is of a class implementing more than one of the interfaces named above. 
       */
        SQL("set-none {p}").on("p" -> None).
          execute() aka "execution" must beFalse
      }

    "set null parameter from empty option" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        val empty: Option[String] = None
        SQL("set-empty-opt {p}").on("p" -> empty).
          execute() aka "execution" must beFalse
      }

    "accept deprecated untyped name when feature enabled" in withConnection() {
      implicit c =>
        import anorm.features.parameterWithUntypedName

        val name: Any = "untyped"
        SQL("set-old {untyped}").on(name -> 2l) aka "query" must beLike {
          case q @ SimpleSql(
            SqlQuery("set-old %s", "untyped" :: Nil, _), ps, _) if (
            ps contains "untyped") => q.execute() aka "execution" must beFalse

        }
    }

    "accept deprecated untyped value when feature enabled" in withConnection() {
      implicit c =>
        import anorm.features.anyToStatement

        val d = new java.util.Date()
        val params: Seq[NamedParameter] = Seq("mod" -> d, "id" -> "idv")
        SQL("UPDATE item SET last_modified = {mod} WHERE id = {id}").
          on(params: _*) aka "update" must beLike {
            case q @ SimpleSql(
              SqlQuery("UPDATE item SET last_modified = %s WHERE id = %s",
                "mod" :: "id" :: Nil, _), ps, _) if (ps.contains("mod") &&
              ps.contains("id")) => q.execute() aka "execution" must {
              throwA[SQLFeatureNotSupportedException](
                message = "Unsupported parameter type: java.util.Date")
            }
          }
    }

    "accept value wrapped as opaque parameter object" in withConnection() {
      implicit c =>
        SQL("set-date {d}").on('d -> anorm.Object(new java.util.Date())).
          execute aka "execution" must throwA[SQLFeatureNotSupportedException](
            message = "Unsupported parameter type: java.util.Date")

    }

    "set sequence values" in withConnection() { implicit c =>
      SQL("set-seq {seq}").
        on('seq -> Seq("a", "b", "c")) aka "query" must beLike {
          case q @ SimpleSql(
            SqlQuery("set-seq %s", "seq" :: Nil, _), ps, _) if (
            ps.size == 1 && ps.contains("seq")) =>
            q.execute() aka "execution" must beFalse
        }
    }

    "set formatted value from sequence" in withConnection() { implicit c =>
      SQL("set-seqp {p}").on('p ->
        SeqParameter(Seq(1.2f, 23.4f, 5.6f), " OR ", "cat = ")).
        aka("query") must beLike {
          case q @ SimpleSql(
            SqlQuery("set-seqp %s", "p" :: Nil, _), ps, _) if (
            ps.size == 1 && ps.contains("p")) =>
            q.execute() aka "execution" must beFalse
        }
    }

    "set formatted value from sequence with string interpolation" in withConnection() { implicit c =>
      SQL"""set-seqp ${SeqParameter(Seq(1.2f, 23.4f, 5.6f), " OR ", "cat = ")}""".
        aka("query") must beLike {
          case q @ SimpleSql(
            SqlQuery("set-seqp %s", "_0" :: Nil, _), ps, _) if (
            ps.size == 1 && ps.contains("_0")) =>
            q.execute() aka "execution" must beFalse
        }
    }
  }

  "Parameter in order" should {
    "be one string" in withConnection() { implicit c =>
      SQL("set-str {p}").onParams(pv("string")) aka "query" must beLike {
        case q @ SimpleSql( // check accross construction
          SqlQuery("set-str %s", List("p"), _), ps, _) if (ps contains "p") =>

          // execute = false: update ok but returns no resultset
          // see java.sql.PreparedStatement#execute
          q.execute() aka "execution" must beFalse
      }
    }

    "be boolean true" in withConnection() { implicit c =>
      SQL("set-true {p}").onParams(pv(true)).execute() must beFalse
    }

    "be boolean false" in withConnection() { implicit c =>
      SQL("set-false {p}").onParams(pv(false)).execute() must beFalse
    }

    "be short" in withConnection() { implicit c =>
      SQL("set-short {p}").onParams(pv(3.toShort)).execute() must beFalse
    }

    "be byte" in withConnection() { implicit c =>
      SQL("set-byte {p}").onParams(pv(4.toByte)).execute() must beFalse
    }

    "be long" in withConnection() { implicit c =>
      SQL("set-long {p}").onParams(pv(5l)).execute() must beFalse
    }

    "be float" in withConnection() { implicit c =>
      SQL("set-float {p}").onParams(pv(1.23f)).execute() must beFalse
    }

    "be double" in withConnection() { implicit c =>
      SQL("set-double {p}").onParams(pv(23.456d)).execute() must beFalse
    }

    "be one Java big integer" in withConnection() { implicit c =>
      SQL("set-jbi {p}").onParams(pv(jbi1)).execute() must beFalse
    }

    "be one Scala big integer" in withConnection() { implicit c =>
      SQL("set-sbi {p}").onParams(pv(sbi1)).execute() must beFalse
    }

    "be one Java big decimal" in withConnection() { implicit c =>
      SQL("set-jbd {p}").onParams(pv(jbd1)).execute() must beFalse
    }

    "be one Scala big decimal" in withConnection() { implicit c =>
      SQL("set-sbd {p}").onParams(pv(sbd1)).execute() must beFalse
    }

    "be one date" in withConnection() { implicit c =>
      SQL("set-date {p}").onParams(pv(date)).execute() must beFalse
    }

    "be one timestamp" in withConnection() { implicit c =>
      SQL("set-timestamp {p}").onParams(pv(timestamp)).execute() must beFalse
    }

    "be Id of string" in withConnection() { implicit c =>
      SQL("set-id-str {p}").onParams(pv(Id("str"))).execute() must beFalse
    }

    "be not assigned" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>

        SQL("set-not-assigned {p}").onParams(pv(NotAssigned)).
          execute() must beFalse
      }

    "be multiple (string, Java big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-jbd {a}, {b}").onParams(pv("string"), pv(jbd1)).
          aka("query") must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-jbd %s, %s", List("a", "b"), _), ps, _) if (
              ps.contains("a") && ps.contains("b")) =>
              q.execute() aka "execution" must beFalse

          }
    }

    "be multiple (string, Scala big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-sbd {a}, {b}").onParams(pv("string"), pv(sbd1)).
          execute() aka "execution" must beFalse

    }

    "be defined string option as Some[String]" in withConnection() {
      implicit c =>
        SQL("set-some-str ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Some("string"))).execute() aka "execution" must beFalse
    }

    "be defined string option as Option[String]" in withConnection() {
      implicit c =>
        SQL("set-some-str ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Option("string"))).
          execute() aka "execution" must beFalse
    }

    "be defined Java big decimal option" in withConnection() { implicit c =>
      SQL("set-some-jbd {p}").
        onParams(pv(Some(jbd1))).execute() aka "execute" must beFalse

    }

    "be defined Scala big decimal option" in withConnection() { implicit c =>
      SQL("set-some-sbd {p}").onParams(pv(Some(sbd1))).
        execute() aka "execute" must beFalse

    }

    "set null parameter from None" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        /*
       http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
       */
        SQL("set-none {p}").onParams(pv(None)).
          execute() aka "execution" must beFalse
      }

    "set null parameter from empty option" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        val empty: Option[String] = None
        SQL("set-empty-opt {p}").onParams(pv(empty)).
          execute() aka "execution" must beFalse
      }

    "not be set if placeholder not found in SQL" in withConnection() {
      implicit c =>
        SQL("no-param-placeholder").onParams(pv("not set")).execute().
          aka("execution") must beFalse

    }

    "be partially set if matching placeholder is missing for second one" in {
      withConnection() { implicit c =>
        SQL("no-snd-placeholder ?").copy(argsInitialOrder = List("p")).
          onParams(pv("first"), pv("second")).execute() must beFalse

      }
    }
  }

  private def pv[A](v: A)(implicit s: ToSql[A] = null, p: ToStatement[A]) =
    ParameterValue(v, s, p)
}
