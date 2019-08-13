//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//

//#jdbc-java-dependencies
libraryDependencies ++= Seq(
  javaJdbc
)
//#jdbc-java-dependencies

//#jdbc-scala-dependencies
libraryDependencies ++= Seq(
  jdbc
)
//#jdbc-scala-dependencies

//#jdbc-driver-dependencies
libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.41"
)
//#jdbc-driver-dependencies
