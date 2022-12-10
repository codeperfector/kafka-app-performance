import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    kotlin("jvm") version "1.6.0"
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "org.codeperfector"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val promClientVersion = "0.16.0"

dependencies {
    implementation("io.prometheus:simpleclient:$promClientVersion")
    implementation("io.prometheus:simpleclient_hotspot:$promClientVersion")
    implementation("io.prometheus:simpleclient_httpserver:$promClientVersion")
    implementation("org.apache.kafka:kafka-clients:3.3.1")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.4")
    implementation("ch.qos.logback:logback-classic:1.4.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "MainKt"))
        }
    }
}