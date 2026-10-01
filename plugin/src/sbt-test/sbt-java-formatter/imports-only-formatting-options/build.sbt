import sbt.util.CacheImplicits._

ThisBuild / javafmtFormatterCompatibleJavaVersion := 11

@transient
val rememberImportsCache = taskKey[Unit]("Remember the effective import-only cache input.")
@transient
val expectSameImportsCache = taskKey[Unit]("Check that full-formatting settings do not affect the import-only cache.")
val expectedCheckMessage = settingKey[String]("Expected full-format check error.")
@transient
val expectCheckFailure = taskKey[Unit]("Verify the reason the full-format check fails.")

expectedCheckMessage := "1 file must be formatted"

expectCheckFailure := {
  val expected = expectedCheckMessage.value
  javafmtCheckAll.result.value.toEither match {
    case Left(incomplete) =>
      val messages = Incomplete.allExceptions(incomplete).flatMap(error => Option(error.getMessage))
      assert(messages.exists(_.contains(expected)), s"Expected '$expected', got: ${messages.mkString("; ")}")
    case Right(_) => sys.error(s"Expected failure: $expected")
  }
}

rememberImportsCache := {
  val hash = (Compile / javafmt / streams).value.cacheStoreFactory
    .sub("imports-only").make("inputs").read[Long]()
  IO.write(target.value / "imports-cache-input", hash.toString)
}

expectSameImportsCache := {
  val hash = (Compile / javafmt / streams).value.cacheStoreFactory
    .sub("imports-only").make("inputs").read[Long]()
  assert(hash.toString == IO.read(target.value / "imports-cache-input"),
    "Full-formatting settings changed the import-only cache input.")
}
