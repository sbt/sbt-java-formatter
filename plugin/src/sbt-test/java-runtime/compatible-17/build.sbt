ThisBuild / javafmtFormatterCompatibleJavaVersion := 17

val expectedCheckMessage = settingKey[String]("Expected unsupported full-formatting option error.")
@transient
val expectCheckFailure = taskKey[Unit]("Verify the reason the full-format check fails.")

expectedCheckMessage :=
  "A custom javafmtMaxLineLength requires ThisBuild / javafmtFormatterCompatibleJavaVersion := 21"

expectCheckFailure := {
  val expected = expectedCheckMessage.value
  javafmtCheckAll.result.value.toEither match {
    case Left(incomplete) =>
      val messages = Incomplete.allExceptions(incomplete).flatMap(error => Option(error.getMessage))
      assert(messages.exists(_.contains(expected)), s"Expected '$expected', got: ${messages.mkString("; ")}")
    case Right(_) => sys.error(s"Expected failure: $expected")
  }
}
