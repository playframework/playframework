// Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>

lazy val root: Project = (project in file("."))
  .enablePlugins(PlayScala)
  // Use sbt default layout
  .disablePlugins(PlayLayoutPlugin)
