plugins {
  id("org.playframework.play")
}

repositories {
  mavenCentral()
  mavenLocal()
}

dependencies {
  implementation(project(":nonplay"))
  implementation(project(":common"))
}
