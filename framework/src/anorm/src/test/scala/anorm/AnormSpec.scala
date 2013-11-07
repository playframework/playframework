package anorm

import org.specs2.mutable.Specification

object AnormSpec extends Specification with H2Database {

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
  }

}
