plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "me.f1nal"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":Decompiler"))
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.guava:guava:33.1.0-jre")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.thoughtworks.xstream:xstream:1.4.20")
    implementation("org.tukaani:xz:1.9")
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-analysis:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.ow2.asm:asm-util:9.7")
    implementation("io.github.spair:imgui-java-app:1.86.11")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "me.f1nal.trinity.Main"
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    configurations = listOf(project.configurations.runtimeClasspath.get())
    manifest {
        attributes["Main-Class"] = "me.f1nal.trinity.Main"
    }
    mergeServiceFiles()
}

tasks.register<JavaExec>("run") {
    mainClass.set("me.f1nal.trinity.Main")
    classpath = sourceSets["main"].runtimeClasspath
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}