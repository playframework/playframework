logLevel := Level.Warn

resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.4")

addSbtPlugin( "com.typesafe.sbt" % "sbt-scalariform" % "1.0.1") 

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.1.0")

