/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
import java.time.Duration
import java.util.Base64
import java.util.Properties
import kotlin.text.Charsets.UTF_8

plugins {
    alias(libs.plugins.gradle.plugin.publish)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.spotless)
    signing
}

val playVersion: String =
    Properties().apply {
        val file = file("$projectDir/../../version.properties")
        if (!file.exists()) throw GradleException("Generate Play version file by `sbt savePlayVersion` command")
        file.inputStream().use { load(it) }
        if (this.getProperty("play.version").isNullOrEmpty()) throw GradleException("`play.version` key didn't find in ${file.absolutePath}")
    }.getProperty("play.version")

val isRelease = !playVersion.endsWith("SNAPSHOT")

group = "org.playframework"
version = playVersion

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("org.playframework:play-routes-compiler_2.13:$playVersion")
    testImplementation(libs.assertj)
    testImplementation(libs.commons.io)
    testImplementation(libs.freemarker)
}

tasks.jar {
    manifest {
        attributes("Implementation-Version" to version)
    }
}

@Suppress("UnstableApiUsage")
testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
            targets {
                all {
                    testTask.configure {
                        systemProperty("play.version", playVersion)
                        project.findProperty("scala.version")?.let { scalaVersion ->
                            val ver = (scalaVersion as String).trimEnd { !it.isDigit() }
                            systemProperty("scala.version", ver)
                        }
                    }
                }
            }
        }
    }
}

signing {
    isRequired = isRelease
    if (isRelease) {
        val signingKey =
            Base64.getDecoder().decode(System.getenv("PGP_SECRET").orEmpty()).toString(UTF_8)
        val signingPassword = System.getenv("PGP_PASSPHRASE").orEmpty()
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
}

nexusPublishing {
    packageGroup.set(project.group.toString())
    clientTimeout.set(Duration.ofMinutes(60))
    this.repositories {
        sonatype()
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website.set("https://www.playframework.com/")
    vcsUrl.set("https://github.com/playframework/playframework")
    val play by plugins.creating {
        id = "org.playframework.play"
        displayName = "Play Plugin"
        description = "A Gradle plugin to develop Play application."
        tags.set(listOf("playframework", "web", "template", "java", "scala"))
        implementationClass = "play.gradle.plugin.PlayPlugin"
    }
}

val headerLicense = "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
val headerLicenseHash = "# $headerLicense"
val headerLicenseJava = "/*\n * $headerLicense\n */"

spotless {
    java {
        googleJavaFormat()
        licenseHeader(headerLicenseJava)
    }
    kotlinGradle {
        licenseHeader(headerLicenseJava, "[^/*]")
    }
    format("properties") {
        target("**/*.properties")
        targetExclude("gradle/**")
        licenseHeader(headerLicenseHash, "[^#]")
    }
}
