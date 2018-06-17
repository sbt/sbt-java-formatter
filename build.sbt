import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

lazy val sbtJavaFormatter = project
  .in(file("."))
  .aggregate(plugin)
  .settings(skip in publish := true)

lazy val plugin = project
  .in(file("plugin"))
  .settings(
    organization := "com.lightbend.sbt",
    name := "sbt-java-formatter",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "com.google.googlejavaformat" % "google-java-format" % "1.6"
    ),

    organizationName := "Lightbend Inc.",
    startYear := Some(2015),
    description := "Formats Java code in your project.",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),

    bintrayRepository := "sbt-plugins",
    bintrayOrganization := None,

    scalacOptions ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-unchecked", "-deprecation", "-feature"),
    javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6"),

    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(DanglingCloseParenthesis, Preserve)
      .setPreference(AlignParameters, true),
  )
  .settings(
    ScriptedPlugin.projectSettings,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
  .enablePlugins(AutomateHeaderPlugin)
