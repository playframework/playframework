//
// Copyright (C) 2009-2017 Lightbend Inc. <https://www.lightbend.com>
//

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  javaJpa,
  "org.hibernate" % "hibernate-entitymanager" % "5.1.0.Final" // replace by your jpa implementation
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResourcesExcludes += "META-INF/persistence.xml"
//#jpa-externalize-resources
