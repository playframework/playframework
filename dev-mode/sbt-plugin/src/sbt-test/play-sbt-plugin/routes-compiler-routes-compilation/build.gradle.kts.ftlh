plugins {
  id("org.playframework.play")
}

repositories {
  mavenCentral()
  mavenLocal()
  maven("https://central.sonatype.com/repository/maven-snapshots/")
  maven("https://repository.apache.org/snapshots/")
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
