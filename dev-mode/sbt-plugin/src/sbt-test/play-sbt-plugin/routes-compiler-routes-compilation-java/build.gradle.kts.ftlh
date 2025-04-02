plugins {
  id("org.playframework.play")
}

repositories {
  mavenCentral()
  mavenLocal()
}

sourceSets {
  test {
    scala {
      setSrcDirs(listOf("tests"))
    }
  }
}

dependencies {
  implementation("org.playframework:play-pekko-http-server_${scalaVersion}:${playVersion}")
  implementation("org.playframework:play-guice_${scalaVersion}:${playVersion}")

  testImplementation("org.playframework:play-test_${scalaVersion}:${playVersion}")
  testImplementation("org.assertj:assertj-core:3.26.3")
  testImplementation("org.java-websocket:Java-WebSocket:1.5.7")
}
