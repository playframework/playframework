package anorm

object BatchSqlSpec extends org.specs2.mutable.Specification {
  "Batch SQL" title

  "Creation" should {
    "fail with parameter maps not having same names" in {
      lazy val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}", List("a")),
        Seq(Seq[NamedParameter]("a" -> 0),
          Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch aka "creation" must throwA[IllegalArgumentException](
        message = "Unexpected parameter names: a, b != expected a")
    }

    "fail with names not matching query placeholders" in {
      lazy val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatch("a" -> 0).
        aka("append") must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a, b, c not matching a, b")
    }

    "be successful with parameter maps having same names" in {
      lazy val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
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
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch.addBatch("a" -> 0, "c" -> 4).
        aka("append") must throwA[IllegalArgumentException](
          message = "Unexpected parameter name: c != expected a, b")
    }

    "fail with missing names" in {
      val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
          List("a", "b", "c")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatch("a" -> 0).
        aka("append") must throwA[IllegalArgumentException](
          message = "Missing parameters: b, c")
    }

    "with first parameter map" >> {
      "fail if parameter names not matching query placeholders" in {
        val batch = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

        batch.addBatch("a" -> 0).
          aka("append") must throwA[IllegalArgumentException](
            message = "Expected parameter names don't correspond to placeholders in query: a not matching a, b")

      }

      "be successful" in {
        val b1 = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

        lazy val b2 = b1.addBatch("a" -> 0, "b" -> 1)

        lazy val expectedMaps =
          Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

        (b2 aka "append" must not(throwA[Throwable])).
          and(b2.params aka "parameters" must_== expectedMaps)
      }
    }

    "be successful" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
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
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2)))

      batch.addBatchList(Seq(Seq("a" -> 0, "c" -> 4))).
        aka("append all") must throwA[IllegalArgumentException](
          message = "Unexpected parameter name: c != expected a, b")
    }

    "fail with missing names" in {
      val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
          List("a", "b", "c")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatchList(Seq(Seq("a" -> 0))).
        aka("append all") must throwA[IllegalArgumentException](
          message = "Missing parameters: b, c")
    }

    "with first parameter map" >> {
      "be successful" in {
        val b1 = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

        lazy val b2 = b1.addBatchList(Seq(Seq("a" -> 0, "b" -> 1)))

        lazy val expectedMaps =
          Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

        (b2 aka "append all" must not(throwA[Throwable])).
          and(b2.params aka "parameters" must_== expectedMaps)
      }

      "fail with parameter maps not having same names" in {
        val b1 = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a}", List("a")))

        lazy val b2 = b1.addBatchList(Seq(
          Seq("a" -> 0), Seq("a" -> 1, "b" -> 2)))

        b2 aka "creation" must throwA[IllegalArgumentException](
          message = "Unexpected parameter names: a, b != expected a")
      }

      "fail with names not matching query placeholders" in {
        val b1 = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
            List("a", "b", "c")))

        lazy val b2 = b1.addBatchList(Seq(Seq("a" -> 0)))

        b2 aka "append" must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a not matching a, b, c")
      }

      "be successful with parameter maps having same names" in {
        val b1 = BatchSql(
          SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

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
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")),
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
    "fail with missing names" in {
      lazy val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
          List("d", "b", "c")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatchParams(0).
        aka("append") must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a, b, c not matching d, b, c")
    }

    "be successful with first parameter map" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

      lazy val b2 = b1.addBatchParams(0, 1)
      lazy val expectedMaps =
        Seq(Map[String, ParameterValue]("a" -> 0, "b" -> 1))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }

    "fail with missing argument" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b"))).
        addBatchParams(0, 1)

      lazy val b2 = b1.addBatchParams(2)

      b2 aka "append" must throwA[IllegalArgumentException](
        message = "Missing parameters: b")
    }

    "be successful" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

      lazy val b2 = b1.addBatchParams(0, 1).addBatchParams(2, 3)
      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }
  }

  "Appending list of list of parameter values" should {
    "fail with missing names" in {
      lazy val batch = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a} AND b = {b} AND c = {c}",
          List("d", "b", "c")),
        Seq(Seq[NamedParameter]("a" -> 1, "b" -> 2, "c" -> 3)))

      batch.addBatchParamsList(Seq(Seq(4, 5, 6))).
        aka("append") must throwA[IllegalArgumentException](
          message = "Expected parameter names don't correspond to placeholders in query: a, b, c not matching d, b, c")
    }

    "be successful with first parameter map" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

      lazy val b2 = b1.addBatchParamsList(Seq(Seq(0, 1), Seq(2, 3)))
      lazy val expectedMaps = Seq(
        Map[String, ParameterValue]("a" -> 0, "b" -> 1),
        Map[String, ParameterValue]("a" -> 2, "b" -> 3))

      (b2 aka "append" must not(throwA[Throwable])).
        and(b2.params aka "parameters" must_== expectedMaps)
    }

    "fail with missing argument" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b"))).
        addBatchParams(0, 1)

      lazy val b2 = b1.addBatchParamsList(Seq(Seq(2)))

      b2 aka "append" must throwA[IllegalArgumentException](
        message = "Missing parameters: b")
    }

    "be successful" in {
      val b1 = BatchSql(
        SqlQuery("SELECT * FROM tbl WHERE a = {a}, b = {b}", List("a", "b")))

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
}
