package anorm

import org.specs2.mutable.Specification

object AnormSpec extends Specification with H2Database with AnormTest {

  "anorm" should {
    "allow inserting and retrieving data" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 10L, 'foo -> "Hello", 'bar -> 20)
        .execute()

      SQL("select * from test where id = {id}")
        .on('id -> 10L)
        .map(row =>
          row[String]("foo") -> row[Int]("bar")
        ).list() must_== List(("Hello", 20))
    }

    "returns query result for SELECT" in withConnection { implicit c =>
      createTestTable()
      SQL("insert into test(id, foo, bar) values ({id}, {foo}, {bar})")
        .on('id -> 11L, 'foo -> "World", 'bar -> 21)
        .execute()

      SQL("select * from test where id = {id}")
        .on('id -> 11L).as(testTableParser.singleOpt)
        .aka("result data") must beSome(TestTable(11L, "World", 21))

    }

    "returns result data for stored procedure" in withConnection { implicit c =>
      createAlias("stored_proc", 
        """String proc(String param) {return "Result for " + param;}""")

      SQL("CALL stored_proc({param})")
        .on('param -> "test-proc-1").executeQuery()
        .as(SqlParser.scalar[String].single) must_== "Result for test-proc-1"
    }
  }
}

sealed trait AnormTest { db: H2Database =>
  import SqlParser._

  val testTableParser = long("id") ~ str("foo") ~ int("bar") map {
    case id ~ foo ~ bar => TestTable(id, foo, bar)
  }
}
