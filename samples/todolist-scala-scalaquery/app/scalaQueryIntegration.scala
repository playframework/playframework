package controllers

import play.api.db._
import play.api.mvc._
import play.api.Play.currentApplication

import org.scalaquery.session._ 

trait ScalaQuery {
    self: Controller =>
    
    val database = Database.forDataSource(DB.getDataSource())
    
    def DBAction(a:DBContext => Result):Action = new Action {
        def apply(ctx:Context) = {
            database withSession { session =>
                a(DBContext(ctx,session))
            }
        }
    }
    
    case class DBContext(ctx:Context,db:Session)
    
    implicit def implicitContext(implicit ctx:DBContext) = ctx.ctx
    implicit def implicitDb(implicit ctx:DBContext) = ctx.db
    
}