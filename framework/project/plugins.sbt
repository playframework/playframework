logLevel := Level.Warn

resolvers += Classpaths.typesafeResolver

<<<<<<< .merge_file_dKlKGG
addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")

addSbtPlugin( "com.typesafe.sbtscalariform" % "sbtscalariform" % "0.5.1") 

=======
resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.1")
>>>>>>> .merge_file_gPM9gL
