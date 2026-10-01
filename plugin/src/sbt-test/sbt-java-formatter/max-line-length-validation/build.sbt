ThisBuild / javafmtFormatterCompatibleJavaVersion := 11

val expectedMessage = settingKey[String]("Expected validation error.")
val expectDefaultLineLength = taskKey[Unit]("Verify the default formatter line length.")
val expectFormatFailure = taskKey[Unit]("Verify the formatting validation error.")
val expectCheckFailure = taskKey[Unit]("Verify the check validation error.")

expectedMessage := s"javafmtMaxLineLength must be positive, but was ${javafmtMaxLineLength.value}."

expectDefaultLineLength := assert(javafmtMaxLineLength.value == 100)

def assertFailure[A](result: Result[A], expected: String): Unit = result.toEither match {
  case Left(incomplete) =>
    val messages = Incomplete.allExceptions(incomplete).flatMap(error => Option(error.getMessage))
    assert(messages.exists(_.contains(expected)), s"Expected '$expected', got: ${messages.mkString("; ")}")
  case Right(_) => sys.error(s"Expected failure: $expected")
}

expectFormatFailure := assertFailure((Compile / javafmt).result.value, expectedMessage.value)
expectCheckFailure := assertFailure((Compile / javafmtCheck).result.value, expectedMessage.value)
