plugins {
  id("org.playframework.play")
}

repositories {
  mavenCentral()
  mavenLocal()
}

play {
  routes {
    aggregateReverseRoutes.set(listOf(project(":a"), project(":b"), project(":c")))
  }
}
