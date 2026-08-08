plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("java")
}

group = "me.f1nal"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation(project(":Decompiler"))
    implementation(project(":Application"))
    implementation(project(":MCP"))
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("at.yawk.lz4:lz4-java:1.10.1")
    implementation("com.android.tools.smali:smali-dexlib2:3.0.9")
    implementation("com.android.tools.smali:smali-baksmali:3.0.9")
    implementation("com.android.tools.smali:smali:3.0.9")
    implementation("io.github.skylot:jadx-core:1.5.6")
    implementation("io.github.skylot:jadx-dex-input:1.5.6")
    implementation("com.thoughtworks.xstream:xstream:1.4.21")
    implementation("org.tukaani:xz:1.12")
    implementation("org.ow2.asm:asm:9.9.1")
    implementation("org.ow2.asm:asm-analysis:9.9.1")
    implementation("org.ow2.asm:asm-commons:9.9.1")
    implementation("org.ow2.asm:asm-tree:9.9.1")
    implementation("org.ow2.asm:asm-util:9.9.1")
    implementation("io.github.spair:imgui-java-app:1.92.0")
    implementation("org.lwjgl:lwjgl-nfd:3.4.1")
    implementation("org.lwjgl:lwjgl-nfd:3.4.1:natives-linux")
    implementation("org.lwjgl:lwjgl-nfd:3.4.1:natives-windows")
    implementation("org.lwjgl:lwjgl-nfd:3.4.1:natives-macos")
    implementation("org.lwjgl:lwjgl-nfd:3.4.1:natives-macos-arm64")
}

allprojects {
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
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
    if (System.getProperty("os.name", "").startsWith("Mac")) {
        jvmArgs("-XstartOnFirstThread")
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
}
