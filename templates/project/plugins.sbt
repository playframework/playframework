resolvers += "Typesafe repository" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.sbt" % "sbt-s3" % "0.5")

lazy val plugins = (project in file(".")).dependsOn(playWs)

lazy val playWs = ProjectRef(Path.fileProperty("user.dir").getParentFile / "framework", "Play-WS")
