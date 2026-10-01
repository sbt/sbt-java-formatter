import java.nio.file.Files

ThisBuild / javafmtFormatterCompatibleJavaVersion := 11

@transient
val useValidJavaHome = taskKey[Unit]("Point the formatter Java-home alias to the original JDK.")
@transient
val useMissingJavaLauncher = taskKey[Unit]("Retarget the alias without changing the configured property.")
@transient
val restoreJavaHome = taskKey[Unit]("Restore the original formatter Java-home property.")
@transient
val expectFormatFailure = taskKey[Unit]("Verify that the format cache follows the retargeted alias.")
@transient
val expectCheckFailure = taskKey[Unit]("Verify that the check cache follows the retargeted alias.")
@transient
val expectImportsFailure = taskKey[Unit]("Verify that the import-only format cache follows the retargeted alias.")
@transient
val expectImportsCheckFailure = taskKey[Unit]("Verify that the import-only check cache follows the retargeted alias.")

val originalJavaHomeProperty = sys.props.get("sbt-javafmt.java.home")
val originalJavaLauncher = {
  val home = new File(originalJavaHomeProperty.filter(_.nonEmpty)
    .orElse(sys.env.get("SBT_JAVAFMT_JAVA_HOME").filter(_.nonEmpty))
    .getOrElse(sys.props("java.home")))
  val unixJava = new File(home, "bin/java")
  val launcher = if (unixJava.isFile) unixJava else new File(home, "bin/java.exe")
  assert(launcher.isFile, "The original formatter Java launcher must exist.")
  launcher.getAbsolutePath
}

useValidJavaHome := {
  val alias = (baseDirectory.value / "formatter-home").toPath
  val validHome = baseDirectory.value / "valid-home"
  val launcher = validHome / "bin" / "java"
  IO.createDirectory(launcher.getParentFile)
  val quotedJava = "'" + originalJavaLauncher.replace("'", "'\"'\"'") + "'"
  IO.write(launcher, "#!/bin/sh\nexec " + quotedJava + " \"$@\"\n")
  assert(launcher.setExecutable(true), "The fixture launcher must be executable.")
  Files.deleteIfExists(alias)
  // Directory symlinks stay inside the fixture: sbt cleanup can recursively follow them.
  Files.createSymbolicLink(alias, validHome.toPath)
  val _ = sys.props.put("sbt-javafmt.java.home", alias.toString)
}

useMissingJavaLauncher := {
  val alias = (baseDirectory.value / "formatter-home").toPath
  val missingLauncher = baseDirectory.value / "missing-launcher"
  IO.createDirectory(missingLauncher)
  Files.delete(alias)
  Files.createSymbolicLink(alias, missingLauncher.toPath)
  assert(sys.props("sbt-javafmt.java.home") == alias.toString,
    "Retargeting must not change the configured Java-home path.")
}

restoreJavaHome := {
  val base = baseDirectory.value
  Files.deleteIfExists((base / "formatter-home").toPath)
  originalJavaHomeProperty match {
    case Some(path) => sys.props.put("sbt-javafmt.java.home", path)
    case None => sys.props.remove("sbt-javafmt.java.home")
  }
  assert(new File(originalJavaLauncher).isFile, "The real Java launcher must remain untouched.")
}

def assertMissingLauncher[A](result: Result[A]): Unit = result.toEither match {
  case Left(incomplete) =>
    val messages = Incomplete.allExceptions(incomplete).flatMap(error => Option(error.getMessage))
    assert(messages.exists(_.contains("Could not locate a Java launcher under sbt-javafmt.java.home=")),
      "Expected a missing launcher error, got: " + messages.mkString("; "))
  case Right(_) => sys.error("Expected the retargeted Java-home alias to invalidate cached success.")
}

expectFormatFailure := assertMissingLauncher((Compile / javafmt).result.value)
expectCheckFailure := assertMissingLauncher((Compile / javafmtCheck).result.value)
expectImportsFailure := assertMissingLauncher((Compile / javafmtFixImports).result.value)
expectImportsCheckFailure := assertMissingLauncher((Compile / javafmtFixImportsCheck).result.value)
