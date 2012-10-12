import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "computer-database-jpa"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      java,
      javaJdbc,
      javaJpa,
      "org.hibernate" % "hibernate-entitymanager" % "3.6.9.Final"
    )

    val main = PlayProject(appName, appVersion, appDependencies).settings(
      ebeanEnabled := false   
    )

}
            
