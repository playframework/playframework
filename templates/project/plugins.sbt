resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

val playVersion = Option(System.getProperty("play.version")).getOrElse {
  println("[\033[31merror\033[0m] No play.version system property specified.\n[\033[31merror\033[0m] Just use the build script to launch SBT and life will be much easier.")
  System.exit(1)
  throw new RuntimeException("No play version")
}

libraryDependencies += "com.typesafe.play" %% "play-ws" % playVersion
