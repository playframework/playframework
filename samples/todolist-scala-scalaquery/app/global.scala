
import play.api._

object Global extends GlobalSettings {
    
    override def beforeStart(application:Application) {
        
        import models._
        import play.api.db.Evolutions._
        import org.scalaquery.ql.extended.H2Driver.Implicit._
        
        val ddl = Tasks.ddl
        
        updateEvolutionScript(
            ups = ddl.createStatements.mkString("\n"),
            downs = ddl.dropStatements.mkString("\n")
        )(application)
        
    }
    
}