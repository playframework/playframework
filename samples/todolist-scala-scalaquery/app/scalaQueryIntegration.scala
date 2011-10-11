package controllers

import play.api.db._
import play.api.Play.currentApplication

import org.scalaquery.session._ 

object ScalaQuery {
    
    lazy val database = Database.forDataSource(DB.getDataSource())
    
    def withSession[T](block:Session => T) = database withSession { session =>
        block(session)
    }
    
}