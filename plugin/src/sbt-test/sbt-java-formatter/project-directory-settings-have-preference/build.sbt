lazy val sbtJavaFormatter = project
  .in(file("."))
  .aggregate(inner, innerWithSettingInProject)

lazy val inner = project
  .in(file("inner"))

lazy val innerWithSettingInProject = project
  .in(file("inner-with-setting-in-project"))
