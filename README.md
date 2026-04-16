# [sbt-java-formatter][] [![scaladex-badge][]][scaladex]

[sbt-java-formatter]: https://github.com/sbt/sbt-java-formatter
[scaladex]:           https://index.scala-lang.org/sbt/sbt-java-formatter
[scaladex-badge]:     https://index.scala-lang.org/sbt/sbt-java-formatter/latest.svg

An sbt plugin for formatting Java code. This plugin began as a combination of ideas from this
[blog post](https://ssscripting.wordpress.com/2009/06/10/how-to-use-the-eclipse-code-formatter-from-your-code/)
and this [maven plugin](https://github.com/revelc/formatter-maven-plugin), though it has evolved since.

`google-java-format` relies on internal `jdk.compiler` APIs. On Java 17 and newer, access to those APIs is strongly encapsulated by the module system.

To keep the formatter commands working without requiring manual JVM flags, the plugin runs `google-java-format` in a forked JVM with the required module access flags.

# Usage

Add the plugin to `project/plugins.sbt`:

```scala
addSbtPlugin("com.github.sbt" % "sbt-java-formatter" % --latest version---)
```

For available versions see [releases](https://github.com/sbt/sbt-java-formatter/releases).

* `javafmt` formats Java files
* `javafmtAll` formats Java files for all configurations (`Compile` and `Test` by default)
* `javafmtCheck` fails if files need reformatting
* `javafmtCheckAll` fails if files need reformatting in any configuration (`Compile` and `Test` by default)
* `javafmtFixImports` fixes Java imports only, without applying full formatting
* `javafmtFixImportsAll` fixes Java imports only for all configurations (`Compile` and `Test` by default)
* `javafmtFixImportsCheck` fails if Java imports need fixing
* `javafmtFixImportsCheckAll` fails if Java imports need fixing in any configuration (`Compile` and `Test` by default)

* The `javafmtOnCompile` setting controls whether the formatter kicks in on compile (`false` by default).
* The `javafmtStyle` setting defines the formatting style: Google Java Style (by default) or AOSP style.
* The `javafmtSortImports` setting controls whether imports are sorted (`true` by default).
* The `javafmtRemoveUnusedImports` setting controls whether unused imports are removed (`true` by default).
* The `javafmtReflowLongStrings` setting controls whether long string literals are reflowed (`true` by default).
* The `javafmtFormatJavadoc` setting controls whether Javadoc comments are reformatted (`true` by default).
* The `javafmtFormatterCompatibleJavaVersion` setting selects which `google-java-format` runtime line to use (`21` by default).
* The `javafmtJavaMaxHeap` setting controls the maximum heap passed to the forked `google-java-format` JVM (`Some("256m")` by default).

This plugin requires sbt 1.3.0+.

## Enable in other scopes (eg `IntegrationTest`)

The sbt plugin is enabled by default for the `Test` and `Compile` configurations. Use `JavaFormatterPlugin.toBeScopedSettings` to enable the plugin for the `IntegrationTest` scope and then use `It/javafmt` to format.

```scala
inConfig(IntegrationTest)(JavaFormatterPlugin.toBeScopedSettings)
```

# Configuration

This plugin uses the [Google Java Format](https://github.com/google/google-java-format) library, which makes it quite opinionated and not particularly configurable.

## Formatter JVM

The formatter runs in a forked JVM managed by the plugin.

By default it uses the same Java installation as the sbt process via `java.home`.

To make the plugin launch the formatter with a different Java installation, set either:

- the `sbt-javafmt.java.home` JVM system property
- or the `SBT_JAVAFMT_JAVA_HOME` environment variable

If both are set, `sbt-javafmt.java.home` takes precedence.

The selected Java home must still be compatible with the `google-java-format` runtime line chosen by `javafmtFormatterCompatibleJavaVersion`.

Use `javafmtJavaMaxHeap` to control the maximum heap size passed to that JVM:

```scala
ThisBuild / javafmtJavaMaxHeap := Some("512m")
```

Set it to `None` to disable the explicit heap cap:

```scala
ThisBuild / javafmtJavaMaxHeap := None
```

## Formatter Options

The plugin also exposes a few `google-java-format` CLI options directly:

```scala
ThisBuild / javafmtFormatterCompatibleJavaVersion := 21
ThisBuild / javafmtSortImports := true
ThisBuild / javafmtRemoveUnusedImports := true
ThisBuild / javafmtReflowLongStrings := true
ThisBuild / javafmtFormatJavadoc := true
```

Set any of them to `false` to pass the corresponding `--skip-...` flag to `google-java-format`.

`javafmtFormatterCompatibleJavaVersion` maps to these formatter versions:

- `11` -> `google-java-format 1.24.0`
- `17` -> `google-java-format 1.28.0`
- `21` -> `google-java-format 1.35.0`

`javafmtOptions` is still available for compatibility, but the preferred sbt-facing configuration is through the dedicated `javafmt...` settings above.

`JavaFormatterOptions.reorderModifiers()` currently has no effect in this plugin.

The plugin now runs `google-java-format` via its CLI in a forked JVM, and the released `google-java-format` CLI used here [does not yet support a corresponding `--skip-reordering-modifiers` flag](https://github.com/google/google-java-format/pull/1373).

If you want to tweak the format, take a minute to consider whether it is really worth it, and have a look at the motivations in the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
If you decide you really need more flexibility, you could consider other plugins such as the [sbt-checkstyle-plugin](https://github.com/etsy/sbt-checkstyle-plugin)

# Contributing

Yes, we'll happily accept PRs to improve the plugin.

Take a look at the [contributors graph](https://github.com/sbt/sbt-java-formatter/graphs/contributors) if you want to contact
any of the contributors directly.

# License

Apache v2
