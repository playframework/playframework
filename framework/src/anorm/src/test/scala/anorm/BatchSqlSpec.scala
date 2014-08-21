package anorm

import acolyte.jdbc.AcolyteDSL
import acolyte.jdbc.Implicits._

object BatchSqlSpec
    extends org.specs2.mutable.Specification with H2Database {

  "Batch SQL" title

  "Creation" should {
    "fail with parameter maps not having same names" in {
      lazy val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}",
        Seq(Seq[NamedParameter]("a" -> 0),
          Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch aka "creation" must throwA[IllegalArgumentException](
        message = "Unexpected parameter names: a, b != expected a")
    }

    "fail with names not matching query placeholders" in {
      lazy val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatch("a" -> 0).
        aka("append") must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a, b, c not matching a, b")
    }

    "be successful with parameter maps having same names" in {
      lazy val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq[NamedParameter]("a" -> 0, "b" -> -1),
          Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> -1),
        Map[String, ParameterValue]("a" -> 1, "b" -> 2))

      (batch aka "creation" must not(throwA[IllegalArgumentException](
        message = "Unexpected parameter names: a, b != expected a"))).
        and(batch.params aka "parameters" must_== expectedMaps)
    }
  }

  "Appending list of named parameters" should {
    "fail with unexpected name" in {
      val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch.addBatch("a" -> 0, "c" -> 4).
        aka("append") must throwA[IllegalArgumentException](
          message = "Unexpected parameter name: c != expected a, b")
    }

    "fail with missing names" in {
      val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatch("a" -> 0).
        aka("append") must throwA[IllegalArgumentException](
          message = "Missing parameters: b, c")
    }

    "with first parameter map" >> {
      "fail if parameter names not matching query placeholders" in {
        val batch = BatchSql(
          "SELECT * FROM tbl WHERE a = {a}, b = {b}")

        batch.addBatch("a" -> 0).
          aka("append") must throwA[IllegalArgumentException](
            message = "Expected parameter names don't correspond to placeholders in query: a not matching a, b")

      }

      "be successful" in {
        val b1 = BatchSql(
          "SELECT * FROM tbl WHERE a = {a}, b = {b}")

        lazy val b2 = b1.addBatch("a" -> 0, "b" -> 1)

        lazy val expectedMaps =
          Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

        (b2 aka "append" must not(throwA[Throwable])).
          and(b2.params aka "parameters" must_== expectedMaps)
      }
    }

    "be successful" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq[NamedParameter]("a" -> 0, "b" -> 1)))

      lazy val b2 = b1.addBatch("a" -> 2, "b" -> 3)

      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }
  }

  "Appending list of list of named parameters" should {
    "fail with unexpected name" in {
      val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch.addBatchList(Seq(Seq("a" -> 0, "c" -> 4))).
        aka("append all") must throwA[IllegalArgumentException](
          message = "Unexpected parameter name: c != expected a, b")
    }

    "fail with missing names" in {
      val batch = BatchSql(
        "SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatchList(Seq(Seq("a" -> 0))).
        aka("append all") must throwA[IllegalArgumentException](
          message = "Missing parameters: b, c")
    }

    "with first parameter map" >> {
      "be successful" in {
        val b1 = BatchSql("SELECT * FROM tbl WHERE a = {a}, b = {b}")

        lazy val b2 = b1.addBatchList(Seq(Seq("a" -> 0, "b" -> 1)))

        lazy val expectedMaps =
          Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

        (b2 aka "append all" must not(throwA[Throwable])).
          and(b2.params aka "parameters" must_== expectedMaps)
      }

      "fail with parameter maps not having same names" in {
        val b1 = BatchSql(
          "SELECT * FROM tbl WHERE a = {a}")

        lazy val b2 = b1.addBatchList(Seq(
          Seq("a" -> 0), Seq("a" -> 1, "b" -> 2)))

        b2 aka "creation" must throwA[IllegalArgumentException](
          message = "Unexpected parameter names: a, b != expected a")
      }

      "fail with names not matching query placeholders" in {
        val b1 = BatchSql(
          "SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}")

        lazy val b2 = b1.addBatchList(Seq(Seq("a" -> 0)))

        b2 aka "append" must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a not matching a, b, c")
      }

      "be successful with parameter maps having same names" in {
        val b1 = BatchSql(
          "SELECT * FROM tbl WHERE a = {a}, b = {b}")

        lazy val b2 = b1.addBatchList(Seq(
          Seq[NamedParameter]("a" -> 0, "b" -> -1),
          Seq[NamedParameter]("a" -> 1, "b" -> 2)))

        lazy val expectedMaps = Seq(
          Map[String, ParameterValue]("a" -> 0, "b" -> -1),
          Map[String, ParameterValue]("a" -> 1, "b" -> 2))

        (b2 aka "creation" must not(throwA[IllegalArgumentException](
          message = "Unexpected parameter names: a, b != expected a"))).
          and(b2.params aka "parameters" must_== expectedMaps)
      }
    }

    "be successful" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}",
        Seq(Seq("a" -> 0, "b" -> 1)))

      lazy val b2 = b1.addBatchList(Seq(Seq("a" -> 2, "b" -> 3)))

      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append all" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }
  }

  "Appending list of parameter values" should {
    "be successful with first parameter map" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}")

      lazy val b2 = b1.addBatchParams(0, 1)
      lazy val expectedMaps =
        Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }

    "fail with missing argument" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}").
        addBatchParams(0, 1)

      lazy val b2 = b1.addBatchParams(2)

      b2 aka "append" must throwA[IllegalArgumentException](
        message = "Missing parameters: b")
    }

    "be successful" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}")

      lazy val b2 = b1.addBatchParams(0, 1).addBatchParams(2, 3)
      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }
  }

  "Appending list of list of parameter values" should {
    "be successful with first parameter map" in {
      val b1 = BatchSql("SELECT * FROM tbl WHERE a = {a}, b = {b}")

      lazy val b2 = b1.addBatchParamsList(Seq(Seq(0, 1), Seq(2, 3)))
      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }

    "fail with missing argument" in {
      val b1 = BatchSql(
        "SELECT * FROM tbl WHERE a = {a}, b = {b}").addBatchParams(0, 1)

      lazy val b2 = b1.addBatchParamsList(Seq(Seq(2)))

      b2 aka "append" must throwA[IllegalArgumentException](
        message = "Missing parameters: b")
    }

    "be successful" in {
      val b1 = BatchSql("SELECT * FROM tbl WHERE a = {a}, b = {b}")

      lazy val b2 = b1.addBatchParamsList(Seq(Seq(0, 1))).
        addBatchParamsList(Seq(Seq(2, 3), Seq(4, 5)))

      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3),
        Map[String, ParameterValue]("a" -> 4, "b" -> 5))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }
  }

  "Batch inserting" should {
    "be success on test1 table" in withH2Database { implicit con =>
      createTest1Table()

      lazy val batch = BatchSql(
        "INSERT INTO test1(id, foo, bar) VALUES({i}, {f}, {b})", Seq(
          Seq[NamedParameter]('i -> 1, 'f -> "foo #1", 'b -> 2),
          Seq[NamedParameter]('i -> 2, 'f -> "foo_2", 'b -> 4)))

      (batch.sql.statement aka "parsed statement" mustEqual (
        "INSERT INTO test1(id, foo, bar) VALUES(%s, %s, %s)")).
        and(batch.execute() aka "batch result" mustEqual Array(1, 1))
    }
  }
}
