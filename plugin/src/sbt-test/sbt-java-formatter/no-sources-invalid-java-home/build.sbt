ThisBuild / javafmtFormatterCompatibleJavaVersion := 11
ThisBuild / javafmtOnCompile := true

lazy val root = project.in(file(".")).aggregate(scalaOnly)
lazy val scalaOnly = project.in(file("scala-only"))

val useInvalidJavaHome = taskKey[Unit]("Select a missing formatter Java installation.")
@transient
val useUnresolvableJavaHome = taskKey[Unit]("Select a Java-home path that cannot be resolved.")
val restoreJavaHome = taskKey[Unit]("Restore the original formatter Java installation.")
val addJavaSource = taskKey[Unit]("Add a source that requires the formatter JVM.")
val expectMissingJavaLauncher = taskKey[Unit]("Verify that formatting still validates the launcher.")

val originalJavaHomeProperty = sys.props.get("sbt-javafmt.java.home")

useInvalidJavaHome := {
  val _ = sys.props.put("sbt-javafmt.java.home", (baseDirectory.value / "missing-jdk").getAbsolutePath)
}

useUnresolvableJavaHome := {
  val invalidPath = (baseDirectory.value / "missing-jdk").getAbsolutePath + 0.toChar
  val _ = sys.props.put("sbt-javafmt.java.home", invalidPath)
}

restoreJavaHome := {
  originalJavaHomeProperty match {
    case Some(path) => sys.props.put("sbt-javafmt.java.home", path)
    case None => sys.props.remove("sbt-javafmt.java.home")
  }
  ()
}

addJavaSource := IO.write(baseDirectory.value / "src/main/java/Example.java", "class Example { }\n")

expectMissingJavaLauncher := {
  (Compile / javafmt).result.value.toEither match {
    case Left(incomplete) =>
      val messages = Incomplete.allExceptions(incomplete).flatMap(error => Option(error.getMessage))
      assert(messages.exists(_.contains("Could not locate a Java launcher under sbt-javafmt.java.home=")),
        "Expected a missing launcher error, got: " + messages.mkString("; "))
    case Right(_) => sys.error("Expected formatting to fail for the missing Java launcher.")
  }
}
