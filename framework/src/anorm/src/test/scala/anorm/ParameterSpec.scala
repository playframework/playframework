package anorm

import acolyte.{
  DefinedParameter => DParam,
  ParameterMetaData,
  UpdateExecution
}
import acolyte.Acolyte.{ connection, handleStatement }
import acolyte.Implicits._

object ParameterSpec extends org.specs2.mutable.Specification {
  "Parameter" title

  val jbg1 = new java.math.BigDecimal(1.234d)
  val sbg1 = BigDecimal(jbg1)
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
  val SqlNum1 = ParameterMetaData.Numeric(jbg1)

  def withConnection[A](ps: (String, String)*)(f: java.sql.Connection => A): A = f(connection(handleStatement withUpdateHandler {
    case UpdateExecution("set-str ?",
      DParam("string", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-false ?",
      DParam(false, SqlBool) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-true ?",
      DParam(true, SqlBool) :: Nil) => 1 /* case ok */
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
    case UpdateExecution("set-jbg ?",
      DParam(jbg1, SqlNum1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-sbg ?",
      DParam(sbg1, SqlNum1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-date ?",
      DParam(date, SqlTimestamp) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-timestamp ?",
      DParam(t: java.sql.Timestamp, SqlTimestamp) :: Nil) if t.getNanos == 123456789 => 1 /* case ok */
    case UpdateExecution("set-s-jbg ?, ?",
      DParam("string", SqlStr) :: DParam(jbg1, SqlNum1) :: Nil) => 1 /* ok */
    case UpdateExecution("set-s-sbg ?, ?",
      DParam("string", SqlStr) :: DParam(sbg1, SqlNum1) :: Nil) => 1 /* ok */
    case UpdateExecution("reorder-s-jbg ?, ?",
      DParam(jbg1, SqlNum1) :: DParam("string", SqlStr) :: Nil) => 1 /* ok */
    case UpdateExecution("set-some-str ?",
      DParam("string", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-str-opt ?",
      DParam("some_str", SqlStr) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-some-jbg ?",
      DParam(jbg1, SqlNum1) :: Nil) => 1 /* case ok */
    case UpdateExecution("set-some-sbg ?",
      DParam(sbg1, SqlNum1) :: Nil) => 1 /* case ok */
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

  }, ps: _*))

  "Named parameters" should {
    "be one string with string name" in withConnection() { implicit c =>
      SQL("set-str {a}").on("a" -> "string").
        aka("query") must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-str ?", List("a"), _),
            Seq(NamedParameter("a", _)), _) =>

            // execute = false: update ok but returns no resultset
            // see java.sql.PreparedStatement#execute
            q.execute() aka "execution" must beFalse
        }
    }

    "be one string with symbol name" in withConnection() { implicit c =>
      SQL("set-str {b}").on('b -> "string").
        aka("query") must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-str ?", List("b"), _),
            Seq(NamedParameter("b", _)), _) =>
            q.execute() aka "execution" must beFalse
        }
    }

    "be boolean true" in withConnection() { implicit c =>
      SQL("set-true {p}").on("p" -> true).execute() must beFalse
    }

    "be boolean false" in withConnection() { implicit c =>
      SQL("set-false {p}").on("p" -> false).execute() must beFalse
    }

    "be int" in withConnection() { implicit c =>
      SQL("set-int {p}").on("p" -> 2).execute() must beFalse
    }

    "be short" in withConnection() { implicit c =>
      SQL("set-short {p}").on("p" -> 3.toShort).execute() must beFalse
    }

    "be byte" in withConnection() { implicit c =>
      SQL("set-byte {p}").on("p" -> 4.toByte).execute() must beFalse
    }

    "be long" in withConnection() { implicit c =>
      SQL("set-long {p}").on("p" -> 5l).execute() must beFalse
    }

    "be float" in withConnection() { implicit c =>
      SQL("set-float {p}").on("p" -> 1.23f).execute() must beFalse
    }

    "be double" in withConnection() { implicit c =>
      SQL("set-double {p}").on("p" -> 23.456d).execute() must beFalse
    }

    "be one Java big decimal" in withConnection() { implicit c =>
      SQL("set-jbg {p}").on("p" -> jbg1).execute() must beFalse
    }

    "be one Scala big decimal" in withConnection() { implicit c =>
      SQL("set-sbg {p}").on("p" -> sbg1).execute() must beFalse
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

        SQL("set-not-assigned {p}").on("p" -> NotAssigned).execute() must beFalse
      }

    "be multiple (string, Java big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-jbg {a}, {b}").on("a" -> "string", "b" -> jbg1).
          aka("query") must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-jbg ?, ?", List("a", "b"), _),
              Seq(NamedParameter("a", _), NamedParameter("b", _)), _) =>
              q.execute() aka "execution" must beFalse

          }
    }

    "be multiple (string, Scala big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-sbg {a}, {b}").on("a" -> "string", "b" -> sbg1).
          execute() aka "execution" must beFalse
    }

    "be reordered" in withConnection() { implicit c =>
      SQL("reorder-s-jbg ?, ?").copy(argsInitialOrder = List("b", "a")).
        on("a" -> "string", "b" -> jbg1) aka "query" must beLike {
          case q @ SimpleSql(
            SqlQuery("reorder-s-jbg ?, ?", List("b", "a"), _),
            Seq(NamedParameter("a", _), NamedParameter("b", _)), _) =>
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
      SQL("set-some-jbg {p}").on("p" -> Some(jbg1)).
        execute() aka "execution" must beFalse

    }

    "be defined Scala big decimal option" in withConnection() { implicit c =>
      SQL("set-some-sbg {p}").on("p" -> Some(sbg1)).
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

    "accept deprecated untyped name" in withConnection() { implicit c =>
      import anorm.features.parameterWithUntypedName

      val name: Any = "untyped"
      SQL("set-old {untyped}").on(name -> 2l) aka "query" must beLike {
        case q @ SimpleSql(
          SqlQuery("set-old ?", "untyped" :: Nil, _),
          Seq(NamedParameter("untyped", _)), _) =>
          q.execute() aka "execution" must beFalse

      }
    }
  }

  "Indexed parameters" should {
    "be one string" in withConnection() { implicit c =>
      SQL("set-str ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv("string")) aka "query" must beLike {
          case q @ SimpleSql( // check accross construction
            SqlQuery("set-str ?", List("p"), _),
            Seq(NamedParameter("p", _)), _) =>

            // execute = false: update ok but returns no resultset
            // see java.sql.PreparedStatement#execute
            q.execute() aka "execution" must beFalse
        }
    }

    "be boolean true" in withConnection() { implicit c =>
      SQL("set-true ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(true)).execute() must beFalse
    }

    "be boolean false" in withConnection() { implicit c =>
      SQL("set-false ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(false)).execute() must beFalse
    }

    "be short" in withConnection() { implicit c =>
      SQL("set-short ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(3.toShort)).execute() must beFalse
    }

    "be byte" in withConnection() { implicit c =>
      SQL("set-byte ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(4.toByte)).execute() must beFalse
    }

    "be long" in withConnection() { implicit c =>
      SQL("set-long ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(5l)).execute() must beFalse
    }

    "be float" in withConnection() { implicit c =>
      SQL("set-float ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(1.23f)).execute() must beFalse
    }

    "be double" in withConnection() { implicit c =>
      SQL("set-double ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(23.456d)).execute() must beFalse
    }

    "be one Java big decimal" in withConnection() { implicit c =>
      SQL("set-jbg ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(jbg1)).execute() must beFalse
    }

    "be one Scala big decimal" in withConnection() { implicit c =>
      SQL("set-sbg ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(sbg1)).execute() must beFalse
    }

    "be one date" in withConnection() { implicit c =>
      SQL("set-date ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(date)).execute() must beFalse
    }

    "be one timestamp" in withConnection() { implicit c =>
      SQL("set-timestamp ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(timestamp)).execute() must beFalse
    }

    "be Id of string" in withConnection() { implicit c =>
      SQL("set-id-str ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(Id("str"))).execute() must beFalse
    }

    "be not assigned" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>

        SQL("set-not-assigned ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(NotAssigned)).execute() must beFalse
      }

    "be multiple (string, Java big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-jbg ?, ?").copy(argsInitialOrder = List("a", "b")).
          onParams(pv("string"), pv(jbg1)) aka "query" must beLike {
            case q @ SimpleSql(
              SqlQuery("set-s-jbg ?, ?", List("a", "b"), _),
              Seq(NamedParameter("a", _), NamedParameter("b", _)), _) =>
              q.execute() aka "execution" must beFalse

          }
    }

    "be multiple (string, Scala big decimal)" in withConnection() {
      implicit c =>
        SQL("set-s-sbg ?, ?").copy(argsInitialOrder = List("a", "b")).
          onParams(pv("string"), pv(sbg1)).execute().
          aka("execution") must beFalse

    }

    "be defined string option as Some[String]" in withConnection() {
      implicit c =>
        SQL("set-some-str ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Some("string"))).execute() aka "execution" must beFalse
    }

    "be defined string option as Option[String]" in withConnection() {
      implicit c =>
        SQL("set-some-str ?").copy(argsInitialOrder = List("p")).
          onParams(pv(Option("string"))).execute() aka "execution" must beFalse
    }

    "be defined Java big decimal option" in withConnection() { implicit c =>
      SQL("set-some-jbg ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(Some(jbg1))).execute() aka "execute" must beFalse

    }

    "be defined Scala big decimal option" in withConnection() { implicit c =>
      SQL("set-some-sbg ?").copy(argsInitialOrder = "p" :: Nil).
        onParams(pv(Some(sbg1))).execute() aka "execute" must beFalse

    }

    "set null parameter from None" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        /*
       http://docs.oracle.com/javase/6/docs/api/java/sql/PreparedStatement.html#setObject%28int,%20java.lang.Object%29
       */
        SQL("set-none ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(None)).execute() aka "execution" must beFalse
      }

    "set null parameter from empty option" in withConnection(
      "acolyte.parameter.untypedNull" -> "true") { implicit c =>
        val empty: Option[String] = None
        SQL("set-empty-opt ?").copy(argsInitialOrder = "p" :: Nil).
          onParams(pv(empty)).execute() aka "execution" must beFalse
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

  private def pv[A](v: A)(implicit t: ToStatement[A]) = ParameterValue(v, t)
}
