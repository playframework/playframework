resolvers += Classpaths.typesafeResolver

addSbtPlugin("com.typesafe.sbtscalariform" % "sbtscalariform" % "0.3.0")

ivyLoggingLevel := UpdateLogging.DownloadOnly
