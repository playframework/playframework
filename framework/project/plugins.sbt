logLevel := Level.Warn

resolvers += Classpaths.typesafeResolver

resolvers += Resolver.url("sbt-plugin-releases", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-releases/"))(Resolver.ivyStylePatterns)

addSbtPlugin("com.typesafe" % "sbt-mima-plugin" % "0.1.3")

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.1")
