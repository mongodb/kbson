/*
 * Copyright 2008-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

group = "org.mongodb.kbson"

version = "1.0.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("com.android.library") version "7.3.0" apply false
    id("maven-publish")
    id("signing")

    // Test based plugins
    id("com.diffplug.spotless") version "6.10.0"
    id("io.gitlab.arturbosch.detekt") version "1.21.0"
    id("org.jetbrains.dokka") version "1.6.10"
}

repositories {
    mavenCentral()
    google()
}

@Suppress("UNUSED_VARIABLE")
kotlin {
    targets.all {
        compilations.all { kotlinOptions { freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn" } }
    }

    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        tasks.withType<Test> { useJUnitPlatform() }
    }

    // Note: Android is configured separately below.
    ios()
    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2") }
        }
        val commonTest by getting {}
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(kotlin("reflect"))
                implementation("org.reflections:reflections:0.10.2")
                implementation("org.mongodb:bson:4.7.1")
            }
        }

        val iosX64Main by getting
        val iosX64Test by getting
        val iosArm64Main by getting
        val iosArm64Test by getting

        val iosMain by getting {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
        }
        val iosTest by getting {
            dependencies {
                // Can't set in CommonTest due to https://youtrack.jetbrains.com/issue/KT-49202
                implementation(kotlin("test"))
            }
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
        }

        val macosX64Main by getting
        val macosX64Test by getting
        val macosArm64Main by getting
        val macosArm64Test by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }
        val macosTest by creating {
            dependencies {
                // Can't set in CommonTest due to https://youtrack.jetbrains.com/issue/KT-49202
                implementation(kotlin("test"))
            }
            dependsOn(commonTest)
            macosX64Test.dependsOn(this)
            macosArm64Test.dependsOn(this)
        }
    }

    // Require that all methods in the API have visibility modifiers and return types.
    // Anything inside `org.mongodb.kbson.internal.*` is considered internal
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

// Android configuration
// The Android plugin doesn't have the nice native targets build check and auto disabling, so we
// replicate it here. Otherwise, gradle fails immediately due to the lack of the sdk preventing any
// development.
val hasAndroidSDK =
    !System.getenv("ANDROID_HOME").isNullOrEmpty() ||
        (File("local.properties").exists() && File("local.properties").readText().contains("sdk.dir="))

if (!hasAndroidSDK) {
    project.logger.warn(
        """
          Android SDK cannot be found. Android support disabled.

            To add android support: Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path
            in your project's local properties file at "${rootDir}/local.properties"""".trimIndent())
} else {
    apply(plugin = "com.android.library")
    @Suppress("UNUSED_VARIABLE")
    kotlin {
        android("android") { publishLibraryVariants("release", "debug") }
        sourceSets {
            val androidMain by getting { dependsOn(getByName("jvmMain")) }
            val androidTest by getting { dependsOn(getByName("jvmTest")) }
        }
    }

    @Suppress("UnstableApiUsage")
    configure<com.android.build.gradle.LibraryExtension> {
        namespace = "org.mongodb.kbson"
        compileSdk = 33
        buildToolsVersion = "33.0.0"

        defaultConfig {
            minSdk = 16
            targetSdk = 33

            sourceSets {
                getByName("main") { manifest.srcFile("src/androidMain/AndroidManifest.xml") }
                getByName("androidTest") { java.srcDirs("src/androidTest/kotlin") }
            }
        }

        publishing {
            multipleVariants {
                withSourcesJar()
                withJavadocJar()
                allVariants()
            }
        }

        lint {
            baseline = rootProject.file("config/android/baseline.xml")
            warningsAsErrors = true
            abortOnError = true
        }

        testOptions { unitTests.all { test -> test.useJUnitPlatform() } }
    }
}

// Output summaries for all test environments (jvm, js and native)
tasks.withType<AbstractTestTask> {
    testLogging {
        events =
            setOf(TestLogEvent.FAILED, TestLogEvent.SKIPPED, TestLogEvent.STANDARD_OUT, TestLogEvent.STANDARD_ERROR)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
    ignoreFailures = true // Always try to run all tests for all modules
    addTestListener(
        object : TestListener {
            override fun beforeTest(testDescriptor: TestDescriptor?) {}
            override fun beforeSuite(suite: TestDescriptor?) {}
            override fun afterTest(testDescriptor: TestDescriptor?, result: TestResult?) {}
            override fun afterSuite(d: TestDescriptor?, r: TestResult?) {
                if (d != null && r != null && d.parent == null) {
                    val resultsSummary =
                        """Tests summary:
                    | ${r.testCount} tests,
                    | ${r.successfulTestCount} succeeded,
                    | ${r.failedTestCount} failed,
                    | ${r.skippedTestCount} skipped"""
                            .trimMargin()
                            .replace("\n", "")

                    val border = "=".repeat(resultsSummary.length)
                    logger.lifecycle("\n$border")
                    logger.lifecycle("Test result: ${r.resultType}")
                    logger.lifecycle(resultsSummary)
                    logger.lifecycle("${border}\n")
                }
            }
        })
}
// Handle test resources for ios
tasks.register<Copy>("copyiOSTestResources") {
    from("src/commonTest/resources")
    into("build/bin/iosX64/debugTest/resources")
}

tasks.findByName("iosX64Test")!!.dependsOn("copyiOSTestResources")

spotless {
    java {
        googleJavaFormat("1.12.0")
        importOrder("kotlin", "org", "com", "")
        removeUnusedImports() // removes any unused imports
        trimTrailingWhitespace()
        endWithNewline()
        indentWithSpaces()
        licenseHeaderFile(rootProject.file("config/mongodb.license"))
    }

    kotlinGradle {
        ktfmt("0.39").dropboxStyle().configure { it.setMaxWidth(120) }
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
        licenseHeaderFile(rootProject.file("config/mongodb.license"), "(group|plugins|import|buildscript|rootProject)")
    }

    kotlin {
        target("**/*.kt")
        ktfmt("0.39").dropboxStyle().configure { it.setMaxWidth(120) }
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
        licenseHeaderFile(rootProject.file("config/mongodb.license"))
    }

    format("extraneous") {
        target("*.xml", "*.yml", "*.md")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}

detekt {
    allRules = true // fail build on any finding
    buildUponDefaultConfig = true // preconfigure defaults
    config = rootProject.files("config/detekt/detekt.yml") // point to your custom config defining rules to run,
    // overwriting default behavior
    baseline = rootProject.file("config/detekt/baseline.xml") // a way of suppressing issues before introducing detekt
    source =
        files(
            file("src/commonMain/kotlin"),
            file("src/commonTest/kotlin"),
            file("src/jvmMain/kotlin"),
            file("src/jvmTest/kotlin"),
            file("src/nativeMain/kotlin"),
            file("src/nativeTest/kotlin"),
        )
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true) // observe findings in your browser with structure and code snippets
        xml.required.set(true) // checkstyle like format mainly for integrations like Jenkins
        txt.required.set(false) // similar to the console output, contains issue signature to manually edit
    }
}

/*
 * Git Versioning
 */
val gitVersion: String by lazy {
    val os = org.apache.commons.io.output.ByteArrayOutputStream()
    project.exec {
        commandLine = "git describe --tags --always --dirty".split(" ")
        standardOutput = os
    }
    String(os.toByteArray()).trim()
}

tasks.register("publishSnapshots") {
    group = "publishing"
    description = "Publishes snapshots to Sonatype"
    if (version.toString().endsWith("-SNAPSHOT")) {
        dependsOn(tasks.withType<PublishToMavenRepository>())
    }
}

tasks.register("publishArchives") {
    group = "publishing"
    description = "Publishes a release and uploads to Sonatype / Maven Central"

    doFirst {
        if (gitVersion != version) {
            val cause =
                """
                | Version mismatch:
                | =================
                |
                | $version != $gitVersion
                |
                | The project version does not match the git tag.
                |""".trimMargin()
            throw GradleException(cause)
        } else {
            println("Publishing: ${project.name} : $gitVersion")
        }
    }

    if (gitVersion == version) {
        dependsOn(tasks.withType<PublishToMavenRepository>())
    }
}

val dokkaOutputDir = "$buildDir/dokka"

tasks.getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaHtml") { outputDirectory.set(file(dokkaOutputDir)) }

val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") { delete(dokkaOutputDir) }

val javadocJar =
    tasks.register<Jar>("javadocJar") {
        dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
        archiveClassifier.set("javadoc")
        from(dokkaOutputDir)
    }

publishing {
    repositories {
        maven {
            name = "oss"
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl

            credentials {
                val nexusUsername: String? by project
                val nexusPassword: String? by project
                username = nexusUsername ?: ""
                password = nexusPassword ?: ""
            }
        }
    }
    publications {
        withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set("http://www.mongodb.org")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("Various")
                        organization.set("MongoDB")
                    }
                }
                scm {
                    connection.set("scm:https://github.com/mongodb/kbson.git")
                    developerConnection.set("scm:git@github.com:mongodb/kbson.git")
                    url.set("https://github.com/mongodb/kbson")
                }
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications)
}

tasks.named("check") { dependsOn(":spotlessApply") }
