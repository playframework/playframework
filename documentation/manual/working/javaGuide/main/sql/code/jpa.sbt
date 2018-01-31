//
// Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
//

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  javaJpa,
  "org.hibernate" % "hibernate-core" % "5.2.15.Final" // replace by your jpa implementation
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResources := false
//#jpa-externalize-resources
