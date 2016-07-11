#sbt-java-formatter

[ ![Download](https://api.bintray.com/packages/ktosopl/sbt-plugins/sbt-java-formatter/images/download.svg) ](https://bintray.com/ktosopl/sbt-plugins/sbt-java-formatter/_latestVersion)

An sbt plugin for formating Java code. Ideas from this [blog post](https://ssscripting.wordpress.com/2009/06/10/how-to-use-the-eclipse-code-formatter-from-your-code/) and this [maven plugin](https://github.com/revelc/formatter-maven-plugin).

Usage
-----

1. Add the plugin to `project/plugins.sbt`:

```scala
addSbtPlugin("com.lightbend.sbt" % "sbt-java-formatter"     % JavaFormatterVersion)
```

2. Prepare a `formatting-java.xml` using Eclipse or steal someone's settings.

3. Point the plugin to the formatting rules by setting the apropriate value:

```scala
  javaFormattingSettingsFile := Some(file("java-formatting.xml"))
```

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
