
import play.api._

object Global extends GlobalSettings {
    
    override def beforeStart(application:Application) {
        
        import models._
        import play.api.db.evolutions.Evolutions._
        
        val ddl = Task.evolution
        
        updateEvolutionScript(
            ups = ddl.createStatements.mkString("\n"),
            downs = ddl.dropStatements.mkString("\n")
        )(application)
        
    }
    
}