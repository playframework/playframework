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

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(${javaVersion}))
  }
}

dependencies {
  implementation("org.playframework:play-pekko-http-server_${scalaVersion}:${playVersion}")
  implementation("org.playframework:play-guice_${scalaVersion}:${playVersion}")

  testImplementation("org.playframework:play-specs2_${scalaVersion}:${playVersion}")
}
