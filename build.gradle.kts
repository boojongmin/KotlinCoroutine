import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

plugins {
    kotlin("jvm") version "1.9.20" // Kotlin version to use
    application
}

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("AppKt")
}
