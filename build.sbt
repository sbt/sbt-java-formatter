lazy val scala212 = "2.12.21"
lazy val scala3 = "3.8.3"
ThisBuild / scalaVersion := scala212
ThisBuild / crossScalaVersions := Seq(scala212, scala3)

lazy val sbtJavaFormatter = project.in(file(".")).aggregate(plugin).settings(publish / skip := true)

lazy val plugin = project
  .in(file("plugin"))
  .enablePlugins(SbtPlugin)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(
    name := "sbt-java-formatter",
    homepage := scmInfo.value.map(_.browseUrl),
    scmInfo := Some(
      ScmInfo(url("https://github.com/sbt/sbt-java-formatter"), "scm:git:git@github.com:sbt/sbt-java-formatter.git")),
    developers := List(
      Developer("ktoso", "Konrad 'ktoso' Malawski", "<ktoso@project13.pl>", url("https://github.com/ktoso"))),
    // Compile the plugin against the Java 11-compatible google-java-format API line; actual formatter execution uses a separately selected forked runtime.
    libraryDependencies ++= Seq("com.google.googlejavaformat" % "google-java-format" % "1.35.0"),
    startYear := Some(2015),
    description := "Formats Java code in your project.",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    (pluginCrossBuild / sbtVersion) := {
      scalaBinaryVersion.value match {
        case "2.12" => "1.9.0"
        case _      => "2.0.0-RC11"
      }
    },
    scalacOptions ++= {
      Vector("-encoding", "UTF-8", "-unchecked", "-deprecation", "-feature") ++ (scalaBinaryVersion.value match {
        case "2.12" => Vector("-Xsource:3", "-release:11")
        case _      => Vector("-Wconf:error")
      })
    },
    javacOptions ++= Seq("-encoding", "UTF-8"),
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    scalafmtOnCompile := !insideCI.value)

ThisBuild / organization := "com.github.sbt"
ThisBuild / organizationName := "sbt community"
