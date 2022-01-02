plugins {
    application
    id("dev.fritz2.fritz2-gradle") version "0.13"
    kotlin("plugin.serialization") version "1.5.32"
    kotlin("multiplatform") version "1.5.32"
    id("idea")
}

group = "dev.fritz2"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

application {
    mainClass.set("app.backend.ServerKt")
}

kotlin {
    js(IR) {
        browser {
            runTask {
                devServer = devServer?.copy(
                    port = 9000,
                    proxy = mutableMapOf(
                        "/api/todos" to "http://localhost:8080"
                    )
                )
            }
        }
    }.binaries.executable()

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }

    sourceSets {
        val ktorVersion = "1.6.7"
        val logbackVersion = "1.2.10"
        val serializationVersion = "1.2.1"
        val exposedVersion = "0.37.3"
        val h2Version = "1.4.200"

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-server-netty:$ktorVersion")
                implementation("io.ktor:ktor-serialization:$ktorVersion")
                implementation("ch.qos.logback:logback-classic:$logbackVersion")

                implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
                implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
                implementation("com.h2database:h2:$h2Version")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
        val jsMain by getting {
            dependencies {
                implementation("dev.fritz2:components:0.13")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks {
    getByName<ProcessResources>("jvmProcessResources") {
        dependsOn(getByName("jsBrowserProductionWebpack"))
        val jsBrowserProductionWebpack =
            getByName<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("jsBrowserProductionWebpack")
        from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
    }

    getByName<JavaExec>("run") {
        dependsOn(getByName<Jar>("jvmJar"))
        classpath(getByName<Jar>("jvmJar"))
        classpath(configurations.jvmRuntimeClasspath)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}