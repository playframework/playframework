package models

import java.sql.Date

import play.api.db._
import play.api.Play.current

import org.scalaquery.ql._
import org.scalaquery.ql.TypeMapper._
import org.scalaquery.ql.extended.{ExtendedTable => Table}

import org.scalaquery.ql.extended.H2Driver.Implicit._ 

import org.scalaquery.session._

object Task extends Table[(Long, String, Date, Boolean)]("tasks") {
    
    lazy val database = Database.forDataSource(DB.getDataSource())
    
    def id = column[Long]("id", O PrimaryKey, O AutoInc)
    def name = column[String]("name", O NotNull)
    def dueDate = column[Date]("due_date")
    def done = column[Boolean]("done")
    def * = id ~ name ~ dueDate ~ done
    def noID = name ~ dueDate ~ done
    
    def findAll = database.withSession { implicit db:Session =>
        (for(t <- this) yield t.id ~ t.name).list
    }
    
    def findById(id:Long) = database.withSession { implicit db:Session =>
        (for(t <- this if t.id === id) yield t.name ~ t.dueDate ~ t.done).first
    }
    
    def update(id:Long, values:(String,Date,Boolean)) = database.withSession { implicit db:Session =>
        (for(t <- this if t.id === id) yield t.name ~ t.dueDate ~ t.done).update(values)
    }
    
    def insert(values:(String,Date,Boolean)) = database.withSession { implicit db:Session =>
        noID.insert(values)
    }
    
    def delete(id:Long) = database.withSession { implicit db:Session =>
        this.where(_.id ===  id).delete
    }
    
    def evolution = ddl
    
}
