resolvers ++= Seq(
    DefaultMavenRepository,
    "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
    Classpaths.typesafeSnapshots // TODO Remove _Classpaths.typesafeSnapshots_ when using sbteclipse release version!
)

addSbtPlugin("play" % "sbt-plugin" % "2.0-RC1-SNAPSHOT")
