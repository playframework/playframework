//
// Copyright (C) 2009-2019 Lightbend Inc. <https://www.lightbend.com>
//

//#cache-sbt-dependencies
libraryDependencies ++= Seq(
  cacheApi
)
//#cache-sbt-dependencies

//#ehcache-sbt-dependencies
libraryDependencies ++= Seq(
  ehcache
)
//#ehcache-sbt-dependencies

//#jcache-sbt-dependencies
libraryDependencies += jcache
//#jcache-sbt-dependencies

//#jcache-guice-annotation-sbt-dependencies
libraryDependencies += "org.jsr107.ri" % "cache-annotations-ri-guice" % "1.0.0"
//#jcache-guice-annotation-sbt-dependencies
