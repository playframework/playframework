package scalaguide.sql

import javax.inject.Inject

import play.api.Play.current
import play.api.mvc._
import play.api.db._

class ScalaControllerInject @Inject()(db: Database) extends Controller {

  def index = Action {
    var outString = "Number is "
    val conn = db.getConnection()
    
    try {
      val stmt = conn.createStatement
      val rs = stmt.executeQuery("SELECT 9 as testkey ")
      
      while (rs.next()) {
        outString += rs.getString("testkey")
      }
    } finally {
      conn.close()
    }
    Ok(outString)
  }

}
