name := "websocket-chat"

version := "1.0"

javacOptions += "-Xlint:deprecation"     

lazy val root = (project in file(".")).addPlugins(PlayJava)
