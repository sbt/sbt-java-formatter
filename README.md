# [sbt-java-formatter][] [![scaladex-badge][]][scaladex]

[sbt-java-formatter]: https://github.com/sbt/sbt-java-formatter
[scaladex]:           https://index.scala-lang.org/sbt/sbt-java-formatter
[scaladex-badge]:     https://index.scala-lang.org/sbt/sbt-java-formatter/latest.svg

An sbt plugin for formatting Java code. This plugin began as a combination of ideas from this
[blog post](https://ssscripting.wordpress.com/2009/06/10/how-to-use-the-eclipse-code-formatter-from-your-code/)
and this [maven plugin](https://github.com/revelc/formatter-maven-plugin), though it has evolved since.

# Usage

Add the plugin to `project/plugins.sbt`:

```scala
// Default plugin:
addSbtPlugin("com.github.sbt" % "sbt-java-formatter" % --latest version---)

// Alternative for Java 17+: wraps formatter commands in a fresh sbt JVM,
// so you do not need to configure `--add-opens` manually, see below.
addSbtPlugin("com.github.sbt" % "sbt-java-formatter-add-opens" % --latest version---)
```

For available versions see [releases](https://github.com/sbt/sbt-java-formatter/releases).

The following commands are available:

* `javafmt` formats Java files
* `javafmtAll` formats Java files for all configurations (`Compile` and `Test` by default)
* `javafmtCheck` fails if files need reformatting
* `javafmtCheckAll` fails if files need reformatting in any configuration (`Compile` and `Test` by default)

The `sbt-java-formatter-add-opens` plugin wraps the above commands and, on Java 17+, runs them in a fresh sbt JVM with the required `jdk.compiler` module access flags. From a user perspective, the commands stay the same and no manual JVM flags need to be configured.

* The `javafmtOnCompile` setting controls whether the formatter kicks in on compile (`false` by default).
* The `javafmtStyle` setting defines the formatting style: Google Java Style (by default) or AOSP style.

This plugin requires sbt 1.3.0+.

## Java 17+

`google-java-format` relies on internal `jdk.compiler` APIs. On Java 17 and newer, access to those APIs is strongly encapsulated by the module system.

If you depend on `sbt-java-formatter-add-opens`, the formatter commands (`javafmt`, `javafmtAll`, `javafmtCheck`, `javafmtCheckAll`) automatically relaunch in a JVM with the required module flags, instead of requiring manual `-J--add-opens=...` setup.

## Enable in other scopes (eg `IntegrationTest`)

The sbt plugin is enabled by default for the `Test` and `Compile` configurations. Use `JavaFormatterPlugin.toBeScopedSettings` to enable the plugin for the `IntegrationTest` scope and then use `It/javafmt` to format.

```scala
inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings)
```

# Configuration

This plugin uses the [Google Java Format](https://github.com/google/google-java-format) library, which makes it quite opinionated and not particularly configurable.

If you want to tweak the format, take a minute to consider whether it is really worth it, and have a look at the motivations in the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
If you decide you really need more flexibility, you could consider other plugins such as the [sbt-checkstyle-plugin](https://github.com/etsy/sbt-checkstyle-plugin)

# Contributing

Yes, we'll happily accept PRs to improve the plugin.

Take a look at the [contributors graph](https://github.com/sbt/sbt-java-formatter/graphs/contributors) if you want to contact
any of the contributors directly.

# License

Apache v2
