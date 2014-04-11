name := "computer-database-jpa"

version := "1.0"

libraryDependencies ++= Seq(
  javaJdbc, 
  javaJpa, 
  "org.hibernate" % "hibernate-entitymanager" % "3.6.9.Final"
  )

lazy val root = (project in file(".")).addPlugins(PlayJava)
