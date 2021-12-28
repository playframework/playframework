//
// Copyright (C) Lightbend Inc. <https://www.lightbend.com>
//

//#single-dep
libraryDependencies += "org.apache.derby" % "derby" % "10.13.1.1"
//#single-dep

//#single-dep-test
libraryDependencies += "org.apache.derby" % "derby" % "10.13.1.1" % "test"
//#single-dep-test

//#multi-deps
libraryDependencies ++= Seq(
<<<<<<< HEAD
  "org.apache.derby" % "derby"          % "10.13.1.1",
  "org.hibernate"    % "hibernate-core" % "5.4.30.Final"
=======
  "org.apache.derby" % "derby"          % "10.14.2.0",
  "org.hibernate"    % "hibernate-core" % "5.4.32.Final"
>>>>>>> 5a4aeb0ea1 (Update dependencies)
)
//#multi-deps

//#explicit-scala-version-dep
libraryDependencies += "org.scala-stm" % "scala-stm_2.13" % "0.9.1"
//#explicit-scala-version-dep

//#auto-scala-version-dep
libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.9.1"
//#auto-scala-version-dep

//#resolver
resolvers += Resolver.sonatypeRepo("snapshots")
//#resolver

//#local-maven-repos
resolvers += (
  "Local Maven Repository".at(s"file:///${Path.userHome.absolutePath}/.m2/repository")
)
//#local-maven-repos
