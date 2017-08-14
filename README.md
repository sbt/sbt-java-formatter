# sbt-java-formatter

[ ![Download](https://api.bintray.com/packages/ktosopl/sbt-plugins/sbt-java-formatter/images/download.svg) ](https://bintray.com/ktosopl/sbt-plugins/sbt-java-formatter/_latestVersion) [![Build Status](https://travis-ci.org/typesafehub/sbt-java-formatter.svg?branch=master)](https://travis-ci.org/typesafehub/sbt-java-formatter)

An sbt plugin for formating Java code. Ideas from this [blog post](https://ssscripting.wordpress.com/2009/06/10/how-to-use-the-eclipse-code-formatter-from-your-code/) and this [maven plugin](https://github.com/revelc/formatter-maven-plugin).

Use `0.3.0`+ for sbt 1.0.0, and `0.2.0` for previous versions of sbt.

Usage
-----

1. Add the plugin to `project/plugins.sbt`:

```scala
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter" % JavaFormatterVersion)
```

2. Prepare a `formatting-java.xml` using Eclipse or steal someone's settings.

3. Profit!

There is one optional step, in case you want to name the file differently,
in which case you can do so via:

```scala
  javaFormattingSettingsFilename := "my-little-formatting-settings.xml"
```

File search order in multi-module projects
------------------------------------------
The plugin runs under the assumption that in multi-module projects, the "deeper" projects
may want to specialize their formatting. In other words, the plugin looks up the config
file using the folowing order:

```
1. my-example/project/formatting-java.xml
2. my-example/formatting-java.xml
3. project/formatting-java.xml
4. formatting-java.xml
```

Always defaulting at the "root" project's `project/formatting-java.xml` (and lastly to `formatting-java.xml`).

Contributing
------------

Yes, we'll happily accept PRs to improve the plugin.
Please note that your changes should not accidentally cause reformatting of entire codebases (i.e. by changing defaults etc).

Please note that the plugin is very rough around the edges. It, in its current form was good enough for its initial use case,
and we decided to share it instead of keeping it to ourselfes.

Maintained by
-------------

@bantonsson and/or @ktoso, at @lightbend

License
-------

Apache v2
