import play.api._

import models._
import anorm._

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    InitialData.insert()
  }
  
}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
  
  def insert() = {
    
    if(User.findAll.isEmpty) {
      
      Seq(
        User("guillaume@sample.com", "Guillaume Bort", "secret"),
        User("maxime@sample.com", "Maxime Dantec", "secret"),
        User("sadek@sample.com", "Sadek Drobi", "secret"),
        User("erwan@sample.com", "Erwan Loisant", "secret")
      ).foreach(User.create)
      
      Seq(
        Project(Some(1), "Play framework", "Play 2.0") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
        Project(Some(2), "Play framework", "Play 1.2.4") -> Seq("guillaume@sample.com", "erwan@sample.com"),
        Project(Some(3), "Play framework", "Website") -> Seq("guillaume@sample.com", "maxime@sample.com"),
        Project(Some(4), "Zenexity", "Secret project") -> Seq("guillaume@sample.com", "maxime@sample.com", "sadek@sample.com", "erwan@sample.com"),
        Project(Some(5), "Zenexity", "Playmate") -> Seq("maxime@sample.com"),
        Project(Some(6), "Personal", "Things to do") -> Seq("guillaume@sample.com"),
        Project(Some(7), "Zenexity", "Play samples") -> Seq("guillaume@sample.com", "maxime@sample.com"),
        Project(Some(8), "Personal", "Private") -> Seq("maxime@sample.com"),
        Project(Some(9), "Personal", "Private") -> Seq("guillaume@sample.com"),
        Project(Some(10), "Personal", "Private") -> Seq("erwan@sample.com"),
        Project(Some(11), "Personal", "Private") -> Seq("sadek@sample.com")
      ).foreach {
        case (project,members) => Project.create(project, members)
      }
      
      Seq(
        Task(None, "Todo", 1, "Fix the documentation", false, None, Some("guillaume@sample.com")),
        Task(None, "Urgent", 1, "Prepare the beta release", false, Some(date("2011-11-15")), None),
        Task(None, "Todo", 9, "Buy some milk", false, None, None),
        Task(None, "Todo", 2, "Check 1.2.4-RC2", false, Some(date("2011-11-18")), Some("guillaume@sample.com")),
        Task(None, "Todo", 7, "Finish zentask integration", true, Some(date("2011-11-15")), Some("maxime@sample.com")),
        Task(None, "Todo", 4, "Release the secret project", false, Some(date("2012-01-01")), Some("sadek@sample.com"))
      ).foreach(Task.create)
      
    }
    
  }
  
}
