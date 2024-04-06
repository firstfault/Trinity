plugins {
    id("java")
}

group = "me.f1nal"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.jetbrains:annotations:24.1.0")
    rootProject.dependencies
}

tasks.test {
    useJUnitPlatform()
}