plugins {
    kotlin("jvm") version "1.9.0"
    id("com.github.johnrengelman.shadow").version("7.1.0")
    application
}

group = "me.rooro"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}
dependencies {
    implementation("com.github.InsaneWaifu.Geyser-Utils:minestom:dev-SNAPSHOT")
    implementation("com.github.InsaneWaifu.Geyser-Utils:GeyserUtils:dev-SNAPSHOT")
    implementation("dev.hollowcube:minestom-ce:438338381e")
}


kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.rooro.MainKt"
    }
}