addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.1")
addSbtPlugin("org.scalariform"   % "sbt-scalariform" % "1.8.0")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.0.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray"     % "0.5.4")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"      % "3.0.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
