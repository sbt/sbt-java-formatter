addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.6")
addSbtPlugin("com.github.sbt" % "sbt-header" % "5.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.2")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
