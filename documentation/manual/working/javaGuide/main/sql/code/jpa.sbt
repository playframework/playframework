//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  javaJpa,
<<<<<<< HEAD
  "org.hibernate" % "hibernate-core" % "5.4.30.Final" // replace by your jpa implementation
=======
  "org.hibernate" % "hibernate-core" % "5.4.32.Final" // replace by your jpa implementation
>>>>>>> 5a4aeb0ea1 (Update dependencies)
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml"
//#jpa-externalize-resources
