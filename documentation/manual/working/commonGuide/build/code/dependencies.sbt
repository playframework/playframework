//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#single-dep
libraryDependencies += "org.apache.derby" % "derby" % "10.13.1.1"
//#single-dep

//#single-dep-test
libraryDependencies += "org.apache.derby" % "derby" % "10.13.1.1" % "test"
//#single-dep-test

//#multi-deps
libraryDependencies ++= Seq(
  "org.apache.derby" % "derby"                   % "10.13.1.1",
  "org.hibernate"    % "hibernate-entitymanager" % "5.2.10.Final"
)
//#multi-deps

//#explicit-scala-version-dep
libraryDependencies += "org.scala-stm" % "scala-stm_2.11" % "0.8"
//#explicit-scala-version-dep

//#auto-scala-version-dep
libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.8"
//#auto-scala-version-dep

//#resolver
resolvers += "sonatype snapshots".at("https://oss.sonatype.org/content/repositories/snapshots/")
//#resolver

//#local-maven-repos
resolvers += (
  "Local Maven Repository".at(s"file:///${Path.userHome.absolutePath}/.m2/repository")
)
//#local-maven-repos
