package models

import java.sql.Date

import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ExtendedTable => Table}

object Tasks extends Table[(Long, String, Date, Boolean)]("tasks") {
    def id = column[Long]("id", O PrimaryKey, O AutoInc)
    def name = column[String]("name", O NotNull)
    def dueDate = column[Date]("due_date")
    def done = column[Boolean]("done")
    def * = id ~ name ~ dueDate ~ done
    def noID = name ~ dueDate ~ done
}
