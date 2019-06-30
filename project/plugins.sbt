addSbtPlugin("com.jsuereth"      % "sbt-pgp"         % "1.1.2")
addSbtPlugin("org.scalariform"   % "sbt-scalariform" % "1.8.3")
addSbtPlugin("de.heikoseeberger" % "sbt-header"      % "5.2.0")
addSbtPlugin("org.foundweekends" % "sbt-bintray"     % "0.5.5")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"      % "4.0.0")

libraryDependencies += "org.scala-sbt" %% "scripted-plugin" % sbtVersion.value
