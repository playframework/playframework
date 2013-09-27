package models

import java.util.{Date}

import play.api.db._
import play.api.Play.current

import anorm._
import anorm.SqlParser._

import scala.language.postfixOps

case class Company(id: Pk[Long] = NotAssigned, name: String)
case class Computer(id: Pk[Long] = NotAssigned, name: String, introduced: Option[Date], discontinued: Option[Date], companyId: Option[Long])

/**
 * Helper for pagination.
 */
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Computer {
  
  // -- Parsers
  
  /**
   * Parse a Computer from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("computer.id") ~
    get[String]("computer.name") ~
    get[Option[Date]]("computer.introduced") ~
    get[Option[Date]]("computer.discontinued") ~
    get[Option[Long]]("computer.company_id") map {
      case id~name~introduced~discontinued~companyId => Computer(id, name, introduced, discontinued, companyId)
    }
  }
  
  /**
   * Parse a (Computer,Company) from a ResultSet
   */
  val withCompany = Computer.simple ~ (Company.simple ?) map {
    case computer~company => (computer,company)
  }
  
  // -- Queries
  
  /**
   * Retrieve a computer from the id.
   */
  def findById(id: Long): Option[Computer] = {
    DB.withConnection { implicit connection =>
      SQL("select * from computer where id = {id}").on('id -> id).as(Computer.simple.singleOpt)
    }
  }
  
  /**
   * Return a page of (Computer,Company).
   *
   * @param page Page to display
   * @param pageSize Number of computers per page
   * @param orderBy Computer property used for sorting
   * @param filter Filter applied on the name column
   */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[(Computer, Option[Company])] = {
    
    val offest = pageSize * page
    
    DB.withConnection { implicit connection =>
      
      val computers = SQL(
        """
          select * from computer 
          left join company on computer.company_id = company.id
          where computer.name like {filter}
          order by {orderBy} nulls last
          limit {pageSize} offset {offset}
        """
      ).on(
        'pageSize -> pageSize, 
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy
      ).as(Computer.withCompany *)

      val totalRows = SQL(
        """
          select count(*) from computer 
          left join company on computer.company_id = company.id
          where computer.name like {filter}
        """
      ).on(
        'filter -> filter
      ).as(scalar[Long].single)

      Page(computers, page, offest, totalRows)
      
    }
    
  }
  
  /**
   * Update a computer.
   *
   * @param id The computer id
   * @param computer The computer values.
   */
  def update(id: Long, computer: Computer) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update computer
          set name = {name}, introduced = {introduced}, discontinued = {discontinued}, company_id = {company_id}
          where id = {id}
        """
      ).on(
        'id -> id,
        'name -> computer.name,
        'introduced -> computer.introduced,
        'discontinued -> computer.discontinued,
        'company_id -> computer.companyId
      ).executeUpdate()
    }
  }
  
  /**
   * Insert a new computer.
   *
   * @param computer The computer values.
   */
  def insert(computer: Computer) = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into computer values (
            (select next value for computer_seq), 
            {name}, {introduced}, {discontinued}, {company_id}
          )
        """
      ).on(
        'name -> computer.name,
        'introduced -> computer.introduced,
        'discontinued -> computer.discontinued,
        'company_id -> computer.companyId
      ).executeUpdate()
    }
  }
  
  /**
   * Delete a computer.
   *
   * @param id Id of the computer to delete.
   */
  def delete(id: Long) = {
    DB.withConnection { implicit connection =>
      SQL("delete from computer where id = {id}").on('id -> id).executeUpdate()
    }
  }
  
}

object Company {
    
  /**
   * Parse a Company from a ResultSet
   */
  val simple = {
    get[Pk[Long]]("company.id") ~
    get[String]("company.name") map {
      case id~name => Company(id, name)
    }
  }
  
  /**
   * Construct the Map[String,String] needed to fill a select options set.
   */
  def options: Seq[(String,String)] = DB.withConnection { implicit connection =>
    SQL("select * from company order by name").as(Company.simple *).map(c => c.id.toString -> c.name)
  }
  
}

