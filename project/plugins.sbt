addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.0")
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.7.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "4.1.1")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
