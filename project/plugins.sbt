addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.4")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.10.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.11.1")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
