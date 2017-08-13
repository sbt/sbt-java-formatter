/*
 * Copyright 2016 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

lazy val sbtJavaFormatter = project
  .in(file("."))
  .aggregate(plugin)
  .enablePlugins(NoPublish)

lazy val plugin = project
  .in(file("plugin"))
  .settings(
    name := "sbt-java-formatter",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      Library.eclipseJdtCore,
      Library.eclipseText
    )
  )
  .settings(BintrayPlugin.bintrayPublishSettings: _*)
  .settings(
    ScriptedPlugin.projectSettings,
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false
  )
