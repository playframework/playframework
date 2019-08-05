lazy val root: Project = (project in file("."))
  .enablePlugins(PlayScala)
  // Use sbt default layout
  .disablePlugins(PlayLayoutPlugin)