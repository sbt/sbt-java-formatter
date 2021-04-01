lazy val sbtJavaFormatter = project.in(file(".")).aggregate(plugin).settings(skip in publish := true)

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .settings(
    organization := "com.lightbend.sbt",
    organizationName := "Lightbend Inc.",
    organizationHomepage := Some(url("https://lightbend.com")),
    name := "sbt-java-formatter",
    homepage := Some(url("https://github.com/sbt/sbt-java-formatter")),
    libraryDependencies ++= Seq("com.google.googlejavaformat" % "google-java-format" % "1.10.0"),
    startYear := Some(2015),
    description := "Formats Java code in your project.",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    bintrayRepository := "sbt-plugins",
    bintrayOrganization := Some("ktosopl"),
    crossSbtVersions := List("1.3.0"),
    scalacOptions ++= Seq("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature"),
    javacOptions ++= Seq("-encoding", "UTF-8"),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    scalafmtOnCompile := true,
    // don't do any API docs
    doc / sources := Seq(),
    packageDoc / publishArtifact := false)
  .enablePlugins(AutomateHeaderPlugin)
