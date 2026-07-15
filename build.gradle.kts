import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.badgersmc.votes"
version = findProperty("releaseVersion")?.toString() ?: "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")

    if (providers.gradleProperty("useMavenLocal").orNull == "true") {
        mavenLocal()
    }
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Nexus DI + coroutines bridge (shaded)
    implementation("com.github.BadgersMC.Nexus:nexus-core:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-paper:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-persistence:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-scheduler:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-paper-loader:v2.1.1")
    implementation("com.github.BadgersMC.Nexus:nexus-i18n:v2.1.1")

    // NuVotifier API (provided by server)
    compileOnly(files("libs/nuvotifier-api.jar"))

    // Geyser API for Bedrock forms (provided by server)
    compileOnly("org.geysermc.geyser:api:2.4.2-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.5-SNAPSHOT")
    compileOnly("org.geysermc.cumulus:cumulus:2.0.0-SNAPSHOT")

    // Kotlin + coroutines (downloaded at runtime by PaperLoader)
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Storage (shaded — avoids Nexus loader download issues)
    implementation("org.xerial:sqlite-jdbc:3.45.1.0")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.jetbrains.exposed:exposed-core:0.55.0")
    implementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    implementation("org.jetbrains.exposed:exposed-java-time:0.55.0")

    // Adventure (bundled with Paper)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // PlaceholderAPI (optional hook)
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.127.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("org.jetbrains.exposed:exposed-core:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.55.0")

    // Konsist for SPEAR layer-rule enforcement
    testImplementation("com.lemonappdev:konsist:0.17.3")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("kotlin/**")
    exclude("kotlinx/coroutines/**")
    exclude("META-INF/kotlin*")
    exclude("_COROUTINE/**")
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
