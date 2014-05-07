scalaVersion := Option(System.getProperty("scala.version")).getOrElse("2.10.4")

javaOptions in Test += "-XX:MaxPermSize=128m"