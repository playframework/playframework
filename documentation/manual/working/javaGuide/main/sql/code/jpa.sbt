// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#jpa-sbt-dependencies
libraryDependencies ++= Seq(
  // With Hibernate ORM 7+, list your entities explicitly in persistence.xml.
  // If you prefer automatic entity discovery, add org.hibernate.orm:hibernate-scan-jandex.
  javaJpa,
  "org.hibernate.orm" % "hibernate-core" % "7.3.1.Final" // replace by your jpa implementation
  // If you enable Hibernate Validator, also add a Jakarta EL implementation.
)
//#jpa-sbt-dependencies

//#jpa-externalize-resources
PlayKeys.externalizeResourcesExcludes += baseDirectory.value / "conf" / "META-INF" / "persistence.xml"
//#jpa-externalize-resources
