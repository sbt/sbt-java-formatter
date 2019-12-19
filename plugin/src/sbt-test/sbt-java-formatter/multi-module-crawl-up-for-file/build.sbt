lazy val sbtJavaFormatter = project
  .in(file("."))
  .aggregate(inner)

lazy val inner = project
  .in(file("inner"))

TaskKey[Unit]("removeFile") := {
  val actualPath = (inner / Compile / javaSource).value / "com" / "lightbend" / "GoodFormatting.java"
  java.nio.file.Files.delete(actualPath.toPath)
}
