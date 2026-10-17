plugins {
  id("org.playframework.play")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation(project(":a"))
  implementation(project(":c"))
}
