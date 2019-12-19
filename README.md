# [sbt-java-formatter][] [![scaladex-badge][]][scaladex] [![travis-badge][]][travis]

[sbt-java-formatter]: https://github.com/sbt/sbt-java-formatter
[scaladex]:           https://index.scala-lang.org/sbt/sbt-java-formatter
[scaladex-badge]:     https://index.scala-lang.org/sbt/sbt-java-formatter/latest.svg
[travis]:             https://travis-ci.org/sbt/sbt-java-formatter
[travis-badge]:       https://travis-ci.org/sbt/sbt-java-formatter.svg?branch=master

An sbt plugin for formatting Java code. This plugin began as a combination of ideas from this
[blog post](https://ssscripting.wordpress.com/2009/06/10/how-to-use-the-eclipse-code-formatter-from-your-code/)
and this [maven plugin](https://github.com/revelc/formatter-maven-plugin), though it has evolved since.

Usage
-----

Add the plugin to `project/plugins.sbt`:

```scala
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % --latest version---)
```

For available versions see [releases](https://github.com/sbt/sbt-java-formatter/releases).

* `javafmt` formats Java files (done automatically on `compile` for `Compile` and `Test` configurations, unless `AutomateJavaFormatterPlugin` is disabled)
* `javafmtAll` formats Java files for all configurations (`Compile` and `Test` by default)
* `javafmtCheck` fails if files need reformatting
* `javafmtCheckAll` fails if files need reformatting in any configuration (`Compile` and `Test` by default)


Configuration
-------------

This plugin uses the [Google Java Format](https://github.com/google/google-java-format) library, which makes it quite opinionated and not particularly configurable.

If you want to tweak the format, take a minute to consider whether it is really worth it, and have a look at the motivations in the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html).
If you decide you really need more flexibility, you could consider other plugins such as the [sbt-checkstyle-plugin](https://github.com/etsy/sbt-checkstyle-plugin)

Contributing
------------

Yes, we'll happily accept PRs to improve the plugin, but please note that the plugin is very rough around the edges.

Take a look at the [contributors graph](https://github.com/sbt/sbt-java-formatter/graphs/contributors) if you want to contact
any of the contributors directly.

License
-------

Apache v2
