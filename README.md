<div align="center">
  <a href="https://github.com/firstfault/trinity">
    <img src="screenshots/logo.png" width="200" alt="Trinity logo">
  </a>
  <h1>Trinity</h1>
  <p><strong>One tool to rule them all.</strong></p>
  <p>A next-generation Java reverse-engineering workspace for exploring, understanding, and rewriting bytecode.</p>

  <p>
    <a href="https://github.com/firstfault/trinity/releases"><img alt="GitHub release" src="https://img.shields.io/github/v/release/firstfault/trinity?display_name=tag&include_prereleases&sort=semver&style=flat-square&color=6e78ff"></a>
    <a href="LICENSE"><img alt="License: Apache 2.0" src="https://img.shields.io/badge/license-Apache--2.0-6e78ff?style=flat-square"></a>
    <img alt="Java 17+" src="https://img.shields.io/badge/Java-17%2B-ed8b00?style=flat-square&logo=openjdk&logoColor=white">
    <img alt="Gradle 8.4" src="https://img.shields.io/badge/Gradle-8.4-02303a?style=flat-square&logo=gradle&logoColor=white">
    <a href="https://github.com/firstfault/trinity/stargazers"><img alt="GitHub stars" src="https://img.shields.io/github/stars/firstfault/trinity?style=flat-square&color=f4c542"></a>
  </p>

  <sub>Custom Decompiler &middot; Easy Assembler &middot; Extensive Cross-referencing &middot; Instant Refactoring</sub>
</div>

![Screenshot](screenshots/trinity.png)


## What makes Trinity stand out?

### Decompiler

Trinity approaches decompilation completely differently. Built on the decade-old battle-tested [Fernflower Decompiler](https://www.jetbrains.com/), it integrates directly with the decompiler engine instead of treating the generated Java as plain text. That semantic understanding powers accurate syntax coloring, instant highlighting, navigation, cross-references, editing, and renaming - even when the original bytecode is heavily obfuscated. Trinity's Fernflower fork also fixes numerous bugs and anti-decompilation edge cases.

### Renaming

Rename variables, fields, methods, and classes in place. Trinity updates every known usage across the workspace immediately, so you can turn an unreadable codebase into something understandable as you investigate it.

### Cross-referencing

Trinity's cross-reference search - Xref for short - is instant and exceptionally thorough. It follows references throughout the bytecode, including class literals, annotations, casts, method parameters, return types, field accesses, and invocations. If the JVM can reference it, Trinity is designed to find it.

### Constant search

Search constants across the entire project, not just the strings visible in decompiled source. Strings, numbers, class literals, annotation values, bootstrap arguments, and other class-pool constants are all discoverable.

### Workspaces

Trinity is not limited to opening one JAR at a time. Load one or several archives into a workspace, edit their classes and members, and save the complete project as a compressed Trinity database (`.tdb`) so your analysis is right where you left it.

### Deobfuscation / Refactoring

Built-in refactoring detects likely obfuscated identifiers and replaces names with consistent, readable placeholders. This gives you a clean foundation for understanding the program and applying meaningful names as you go.

### ...and more!

Custom themes, a built-in theme editor, configurable key mappings, among many others. Give Trinity a try and see what else you uncover!

## Building
I haven't bothered with pre-built releases much - but this will come very soon. For now, I highly recommend running Trinity from source:
```bash
git clone https://github.com/firstfault/Trinity.git
cd Trinity
./gradlew run
```
However, there are [binaries (usually outdated) available which you can get from here](https://github.com/firstfault/Trinity/releases).

## Authors
- [@final](https://www.github.com/firstfault)

### Contributing
Contributions are massively appreciated. Please feel free to open an issue or pull request if anything you need is missing.

Please review the [planned features](PLANNED.md) before requesting a feature addition!

### Libraries Used
- [ImGui](https://github.com/ocornut/imgui) with [Bindings](https://github.com/SpaiR/imgui-java)
- [ObjectWeb ASM](https://asm.ow2.io/)
- Modified version of [Fernflower Decompiler](https://www.jetbrains.com/)

#### Who is this meant for?
Hackers, hobbyists, professionals, everyone is welcome.

#### Context
Originally Trinity started out as an obfuscator with an interactive interface. Over time, I began using it as a decompiler, and eventually it evolved into this awesome tool.
