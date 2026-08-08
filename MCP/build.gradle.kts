plugins {
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api(project(":Application"))

    implementation(enforcedPlatform("tools.jackson:jackson-bom:3.1.5"))
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.0")
    implementation("org.eclipse.jetty:jetty-server:12.1.11")
    implementation("org.eclipse.jetty.ee11:jetty-ee11-servlet:12.1.11")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
