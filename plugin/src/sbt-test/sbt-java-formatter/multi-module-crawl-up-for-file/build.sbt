lazy val sbtJavaFormatter = project
  .in(file("."))
  .aggregate(inner)

lazy val inner = project
  .in(file("inner"))
