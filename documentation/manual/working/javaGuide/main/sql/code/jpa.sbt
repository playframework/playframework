// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  javaJpa,
  "org.hibernate.orm" % "hibernate-core" % "7.0.5.Final" // replace by your jpa implementation
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml"
//#jpa-externalize-resources
