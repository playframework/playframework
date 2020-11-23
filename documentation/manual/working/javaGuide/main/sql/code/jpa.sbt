//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  javaJpa,
  "org.hibernate" % "hibernate-core" % "5.4.24.Final" // replace by your jpa implementation
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml"
//#jpa-externalize-resources
