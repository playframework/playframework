/*
 * Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>
 */
import java.time.Duration
import java.util.Base64
import java.util.Properties
import kotlin.text.Charsets.UTF_8

plugins {
    `kotlin-dsl`
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
    gradlePluginPortal()
}

dependencies {
    compileOnly("org.playframework:play-routes-compiler_2.13:$playVersion")
    compileOnly("org.playframework.twirl:gradle-twirl:${libs.versions.twirl.get()}")
    implementation("org.playframework:play-run-support:$playVersion")
    testImplementation(libs.assertj)
    testImplementation(libs.commons.io)
    testImplementation(libs.freemarker)
    testImplementation(libs.mockito)
    testImplementation(libs.kotlin.plugin)
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
    val playJava by plugins.creating {
        id = "org.playframework.play-java"
        displayName = "Play Plugin (Java)"
        description = "A Gradle plugin to develop Play application."
        tags.set(listOf("playframework", "web", "template", "java"))
        implementationClass = "play.gradle.plugin.PlayJavaPlugin"
    }
    val playScala by plugins.creating {
        id = "org.playframework.play-scala"
        displayName = "Play Plugin (Scala)"
        description = "A Gradle plugin to develop Play application."
        tags.set(listOf("playframework", "web", "template", "scala"))
        implementationClass = "play.gradle.plugin.PlayScalaPlugin"
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
        licenseHeader(headerLicenseJava, "(import|rootProject)")
    }
    format("properties") {
        target("**/*.properties")
        targetExclude("gradle/**")
        licenseHeader(headerLicenseHash, "[^#]")
    }
}
