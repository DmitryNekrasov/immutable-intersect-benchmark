plugins {
    kotlin("multiplatform") version "2.4.0"
    id("org.jetbrains.kotlinx.benchmark") version "0.4.17"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:0.4.17")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.0")
            }
        }
    }
}

benchmark {
    targets {
        register("jvm")
    }

    configurations {
        named("main") {
            advanced("jvmForks", 3)
        }
    }
}
