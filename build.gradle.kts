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

group = "org.mongodb"

version = "1.0-SNAPSHOT"

plugins {
    kotlin("multiplatform") version "1.7.0"

    id("com.diffplug.spotless") version "6.10.0"
    id("io.gitlab.arturbosch.detekt").version("1.21.0")
    id("org.jetbrains.dokka") version "1.7.10"
}

repositories { mavenCentral() }

@Suppress("UNUSED_VARIABLE")
kotlin {
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "1.8" }
        withJava()
        tasks.withType<Test> { useJUnitPlatform() }
    }

    js(IR) { nodejs {} }

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget =
        when {
            hostOs == "Mac OS X" -> macosX64("native")
            hostOs == "Linux" -> linuxX64("native")
            isMingwX64 -> mingwX64("native")
            else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
        }

    sourceSets {
        val commonMain by getting
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.10")
                implementation("org.junit.jupiter:junit-jupiter:5.9.0")
                implementation("org.reflections:reflections:0.10.2")
                implementation("org.mongodb:bson:4.7.0")
            }
        }

        val jsMain by getting
        val jsTest by getting

        val nativeMain by getting
        val nativeTest by getting
    }

    // Require that all methods in the API have visibility modifiers and return types.
    // Anything inside `org.kbson.internal.*` is considered internal
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

// Output summaries for all test environments (jvm, js and native)
tasks.withType<AbstractTestTask> {
    testLogging {
        events =
            setOf(
                TestLogEvent.FAILED,
                TestLogEvent.SKIPPED,
                TestLogEvent.STANDARD_OUT,
                TestLogEvent.STANDARD_ERROR)
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
        ktfmt("0.39").dropboxStyle()
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
        licenseHeaderFile(
            rootProject.file("config/mongodb.license"),
            "(group|plugins|import|buildscript|rootProject)")
    }

    kotlin {
        target("**/*.kt")
        ktfmt("0.39").dropboxStyle()
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
    config =
        rootProject.files(
            "config/detekt/detekt.yml") // point to your custom config defining rules to run,
    // overwriting default behavior
    baseline =
        rootProject.file(
            "config/detekt/baseline.xml") // a way of suppressing issues before introducing detekt
    source =
        files(
            file("src/commonMain/kotlin"),
            file("src/commonTest/kotlin"),
            file("src/jvmMain/kotlin"),
            file("src/jvmTest/kotlin"),
            file("src/nativeMain/kotlin"),
            file("src/nativeTest/kotlin"),
            file("src/jsMain/kotlin"),
            file("src/jsTest/kotlin"))

    reports {
        html.enabled = true // observe findings in your browser with structure and code snippets
        xml.enabled = false // checkstyle like format mainly for integrations like Jenkins
        txt.enabled =
            false // similar to the console output, contains issue signature to manually edit
        // baseline files
    }
}

tasks.named("check") { dependsOn(":spotlessApply") }

tasks.named("compileKotlinMetadata") { dependsOn(":spotlessApply") }
