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

Trinity took a completely different stance on decompilation. Built upon the 18 year old battle-tested [Fernflower Decompiler](https://www.jetbrains.com/), it hooks directly into the decompiler to provide you with syntax coloring, instant highlighting, referencing, editing, renaming, you name it. Obfuscated code is handled perfectly, since we directly use the decompilers understanding - not the Java syntax itself to determine what is what. On top of that, many anti-decompilation tricks and bugs were fixed. This makes obfuscated code unimaginably easy to both navigate and handle.

### Renaming

Offers instant rename of variables, fields, methods and classes is instant, updating every single usage everywhere at once - making obfuscated code very easy to understand.

### Cross-referencing

The cross-reference search (or Xref, for short) is one of its kind, with completely instant results, it includes absolutely every single possible reference or constant in the bytecode, unlike any other public reverse engineering tool. Class constants, annotations, checkcasts, parameters, return types - <b>absolutely everything imaginable</b> is covered.

### Constant search

Just like the cross-reference search, nothing is left out. Absolutely every single constant in the class pool is searchable.

### Workspaces

Unlike any other tool, a JAR is not all that Trinity knows. Each JAR (or multiple, if you want!) is loaded into a workspace, savable and editable as a custom Trinity database file (.tdb). This makes adding/editing classes and other members trivial.

### Deobfuscation / Refactoring

Built-in automatic visual refactoring of obfuscated names (with automatic detection for obfuscation!) lets you automatically take very long, hard to understand names into simple, human readable names - letting you rename it to the according matching name afterwards, without too much fuss.

### ...and more!

Themes (with a built-in theme editor), key mappings, and many others. Give it a try and see what else is left to discover!

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
