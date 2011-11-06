package models
import java.util.Date

trait Base[That <: {val id:Long}] {

    var items:Seq[That]
    def date(str:String):java.util.Date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

    def findAll = items
    def findById(id: Long):Option[That] = items.find(_.id == id)

}

case class Project(id:Long, name: String, group: String, members: Option[Seq[User]])
object Project extends Base[Project] {

    var items = Seq(
        Project( 1, "Play 2.0",     "Playframework",    Option(null) ),
        Project( 2, "Play 1.2.3",   "Playframework",    Option(null) ),
        Project( 3, "Play Scala",   "Playframework",    Option(null) ),
        Project( 4, "Secret",       "Zenexity",         Option(null) ),
        Project( 5, "Play samples", "Personal",         Option(null) ),
        Project( 6, "Playmate",     "Personal",         Option(null) )
    )

    def findByUser = items

    var inc:Int = 7
    def getId = {
        inc = inc+1
        inc
    }
    def add(group: String) = {
        Project(getId, "New project", group, Option(null))
    }
}

case class User(id:Long, name: String, mail: String)
object User extends Base[User] {

    var items = Seq(
        User( 1, "Maxime Dantec", "mda@zenexity.com" ),
        User( 2, "Guillaume Bort", "gbo@zenexity.com" ),
        User( 3, "Sadek Drobi", "sdr@zenexity.com" ),
        User( 4, "Erwan Loisant", "elo@zenexity.com" )
    )

}

case class Task(id:Long, title: String, done: Boolean, dueDate: Option[Date], assignedTo: Option[User], project: Project, folder: String)

trait App[That <: {val id:Long; val project: Project; val folder:String}] {
    var items:Seq[That]
    def date(str:String):java.util.Date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)

    def findAll = items
    def findByProject(id: Long):Seq[That] = {
        items.filter( _.project.id == id)
    }
    def findById(id: Long):Option[That] = items.find(_.id == id)
}

object Task extends App[Task] {

    var items = Seq(
        Task( 1 ,"Buy some milk", false, Option(date("2011-12-01")), Option(User.findById(2).get), Project.findById(1).get, "Urgent"),
        Task( 2, "Buy some milk", true,  Option(date("2011-12-06")), Option(null),                 Project.findById(1).get, "Urgent"),
        Task( 3, "Buy some milk", false, Option(null),               Option(User.findById(1).get), Project.findById(1).get, "Todo"),
        Task( 4, "Buy some milk", false, Option(null),               Option(null),                 Project.findById(4).get, "Todo")
    )
}

