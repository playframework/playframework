// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "%PLAY_VERSION%")

// Web plugins
addSbtPlugin("com.typesafe.sbt" % "sbt-coffeescript" % "%COFFEESCRIPT_VERSION%")
addSbtPlugin("com.typesafe.sbt" % "sbt-less" % "%LESS_VERSION%")
addSbtPlugin("com.typesafe.sbt" % "sbt-jshint" % "%JSHINT_VERSION%")
addSbtPlugin("com.typesafe.sbt" % "sbt-rjs" % "%RJS_VERSION%")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "%DIGEST_VERSION%")
addSbtPlugin("com.typesafe.sbt" % "sbt-mocha" % "%MOCHA_VERSION%")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "%SASSIFY_VERSION%")

// Play enhancer - this automatically generates getters/setters for public fields
// and rewrites accessors of these fields to use the getters/setters. Remove this
// plugin if you prefer not to have this feature, or disable on a per project
// basis using disablePlugins(PlayEnhancer) in your build.sbt
addSbtPlugin("com.typesafe.sbt" % "sbt-play-enhancer" % "%ENHANCER_VERSION%")

// Play Ebean support, to enable, uncomment this line, and enable in your build.sbt using
// enablePlugins(PlayEbean).
// addSbtPlugin("com.typesafe.sbt" % "sbt-play-ebean" % "%EBEAN_VERSION%")
