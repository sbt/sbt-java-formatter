addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.6.0")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.12.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
