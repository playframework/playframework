// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

//#single-dep
libraryDependencies += "org.apache.derby" % "derby" % "10.16.1.1"
//#single-dep

//#single-dep-test
libraryDependencies += "org.apache.derby" % "derby" % "10.16.1.1" % "test"
//#single-dep-test

//#multi-deps
libraryDependencies ++= Seq(
  "org.apache.derby" % "derby"          % "10.16.1.1",
  "org.hibernate"    % "hibernate-core" % "6.3.2.Final"
)
//#multi-deps

//#explicit-scala-version-dep
libraryDependencies += "org.scala-stm" % "scala-stm_2.13" % "0.9.1"
//#explicit-scala-version-dep

//#auto-scala-version-dep
libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.9.1"
//#auto-scala-version-dep

//#resolver
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
//#resolver

//#local-maven-repos
resolvers += (
  "Local Maven Repository".at(s"file:///${Path.userHome.absolutePath}/.m2/repository")
)
//#local-maven-repos
