/*
 * Copyright 2015 sbt community
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

package com.github.sbt

import _root_.sbt.Keys._
import _root_.sbt.{ Def, _ }
import com.google.googlejavaformat.java.JavaFormatterOptions
import com.github.sbt.javaformatter.JavaFormatter

@deprecated("Use javafmtOnCompile setting instead", "0.5.1")
object AutomateJavaFormatterPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin && JavaFormatterPlugin

  override def globalSettings: Seq[Def.Setting[?]] = Seq(JavaFormatterPlugin.autoImport.javafmtOnCompile := false)
}

object JavaFormatterPlugin extends AutoPlugin {

  private val JavafmtRuntime = config("javafmtRuntime").hide.extend(Runtime)

  @transient
  private val javafmtFormatterClasspath =
    taskKey[Seq[File]]("Resolved classpath for the forked google-java-format CLI.")

  object autoImport {
    @transient
    val javafmt: TaskKey[Unit] = taskKey("Format Java sources")
    @transient
    val javafmtCheck: TaskKey[Boolean] = taskKey("Fail, if a Java source needs reformatting.")
    @transient
    val javafmtFixImports: TaskKey[Unit] = taskKey("Fix Java imports only, without applying full formatting.")
    @transient
    val javafmtFixImportsCheck: TaskKey[Boolean] = taskKey("Fail, if Java imports need fixing.")
    @transient
    val javafmtAll: TaskKey[Unit] = taskKey(
      "Execute the javafmt task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    @transient
    val javafmtCheckAll: TaskKey[Unit] = taskKey(
      "Execute the javafmtCheck task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    @transient
    val javafmtFixImportsAll: TaskKey[Unit] = taskKey(
      "Execute the javafmtFixImports task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    @transient
    val javafmtFixImportsCheckAll: TaskKey[Unit] = taskKey(
      "Execute the javafmtFixImportsCheck task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    val javafmtOnCompile = settingKey[Boolean]("Format Java source files on compile, off by default.")
    val javafmtFormatterCompatibleJavaVersion =
      settingKey[Int](
        "Selects the google-java-format runtime line by compatible Java version. Supported values: 11, 17, 21.")
    val javafmtStyle =
      settingKey[JavaFormatterOptions.Style]("Define formatting style, Google Java Style (default) or AOSP")
    val javafmtJavaMaxHeap =
      settingKey[Option[String]]("Maximum heap size passed to the forked google-java-format JVM, e.g. Some(\"256m\").")
    val javafmtSortImports =
      settingKey[Boolean]("Whether google-java-format should sort imports. Enabled by default.")
    val javafmtRemoveUnusedImports =
      settingKey[Boolean]("Whether google-java-format should remove unused imports. Enabled by default.")
    val javafmtReflowLongStrings =
      settingKey[Boolean]("Whether google-java-format should reflow long string literals. Enabled by default.")
    val javafmtFormatJavadoc =
      settingKey[Boolean]("Whether google-java-format should format Javadoc comments. Enabled by default.")
    val javafmtOptions = settingKey[JavaFormatterOptions](
      "Compatibility setting for upstream JavaFormatterOptions. Prefer the dedicated javafmt... settings; reorderModifiers() currently has no effect with the released google-java-format CLI used by this plugin.")
  }

  import autoImport._

  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings: Seq[Def.Setting[?]] = {
    val anyConfigsInThisProject = ScopeFilter(configurations = inAnyConfiguration)

    notToBeScopedSettings ++
    Seq(Compile, Test).flatMap(inConfig(_)(toBeScopedSettings)) ++
    Seq(
      javafmtAll := {
        val _ = javafmt.?.all(anyConfigsInThisProject).value
      },
      javafmtCheckAll := {
        val _ = javafmtCheck.?.all(anyConfigsInThisProject).value
      },
      javafmtFixImportsAll := {
        val _ = javafmtFixImports.?.all(anyConfigsInThisProject).value
      },
      javafmtFixImportsCheckAll := {
        val _ = javafmtFixImportsCheck.?.all(anyConfigsInThisProject).value
      })
  }

  override def globalSettings: Seq[Def.Setting[?]] =
    Seq(
      javafmtOnCompile := false,
      javafmtFormatterCompatibleJavaVersion := 21,
      javafmtStyle := JavaFormatterOptions.Style.GOOGLE,
      javafmtJavaMaxHeap := Some("256m"),
      javafmtSortImports := true,
      javafmtRemoveUnusedImports := true,
      javafmtReflowLongStrings := true,
      javafmtFormatJavadoc := true)

  def toBeScopedSettings: Seq[Setting[?]] =
    List(
      (javafmt / sourceDirectories) := List(javaSource.value),
      javafmtOptions := JavaFormatterOptions
        .builder()
        .style(javafmtStyle.value)
        .formatJavadoc(javafmtFormatJavadoc.value)
        .build(),
      javafmt := {
        val streamz = streams.value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = streamz.cacheStoreFactory
        val options = javafmtOptions.value
        val formatterClasspath = javafmtFormatterClasspath.value.toVector
        val javaMaxHeap = javafmtJavaMaxHeap.value
        val sortImports = javafmtSortImports.value
        val removeUnusedImports = javafmtRemoveUnusedImports.value
        val reflowLongStrings = javafmtReflowLongStrings.value
        JavaFormatter(
          sD,
          iF,
          eF,
          streamz,
          cache,
          options,
          formatterClasspath,
          javaMaxHeap,
          sortImports,
          removeUnusedImports,
          reflowLongStrings)
      },
      javafmtCheck := {
        val streamz = streams.value
        val baseDir = (ThisBuild / baseDirectory).value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = (javafmt / streams).value.cacheStoreFactory
        val options = javafmtOptions.value
        val formatterClasspath = javafmtFormatterClasspath.value.toVector
        val javaMaxHeap = javafmtJavaMaxHeap.value
        val sortImports = javafmtSortImports.value
        val removeUnusedImports = javafmtRemoveUnusedImports.value
        val reflowLongStrings = javafmtReflowLongStrings.value
        JavaFormatter.check(
          baseDir,
          sD,
          iF,
          eF,
          streamz,
          cache,
          options,
          formatterClasspath,
          javaMaxHeap,
          sortImports,
          removeUnusedImports,
          reflowLongStrings)
      },
      javafmtFixImports := {
        val streamz = streams.value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = streamz.cacheStoreFactory
        val options = javafmtOptions.value
        val formatterClasspath = javafmtFormatterClasspath.value.toVector
        val javaMaxHeap = javafmtJavaMaxHeap.value
        val sortImports = javafmtSortImports.value
        val removeUnusedImports = javafmtRemoveUnusedImports.value
        val reflowLongStrings = javafmtReflowLongStrings.value
        JavaFormatter.fixImports(
          sD,
          iF,
          eF,
          streamz,
          cache,
          options,
          formatterClasspath,
          javaMaxHeap,
          sortImports,
          removeUnusedImports,
          reflowLongStrings)
      },
      javafmtFixImportsCheck := {
        val streamz = streams.value
        val baseDir = (ThisBuild / baseDirectory).value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = (javafmt / streams).value.cacheStoreFactory
        val options = javafmtOptions.value
        val formatterClasspath = javafmtFormatterClasspath.value.toVector
        val javaMaxHeap = javafmtJavaMaxHeap.value
        val sortImports = javafmtSortImports.value
        val removeUnusedImports = javafmtRemoveUnusedImports.value
        val reflowLongStrings = javafmtReflowLongStrings.value
        JavaFormatter.fixImportsCheck(
          baseDir,
          sD,
          iF,
          eF,
          streamz,
          cache,
          options,
          formatterClasspath,
          javaMaxHeap,
          sortImports,
          removeUnusedImports,
          reflowLongStrings)
      },
      javafmtDoFormatOnCompile := Def.settingDyn {
        if (javafmtOnCompile.value) {
          resolvedScoped.value.scope / javafmt
        } else {
          Def.task(())
        }
      }.value,
      compile / compileInputs := (compile / compileInputs).dependsOn(javafmtDoFormatOnCompile).value)

  def notToBeScopedSettings: Seq[Setting[?]] =
    List(
      ivyConfigurations += JavafmtRuntime,
      libraryDependencies +=
        ("com.google.googlejavaformat" % "google-java-format" % formatterVersionForCompatibleJavaVersion(
          javafmtFormatterCompatibleJavaVersion.value) % JavafmtRuntime.name).classifier("all-deps").intransitive(),
      javafmtFormatterClasspath := update.value
        .matching(
          configurationFilter(JavafmtRuntime.name) && moduleFilter(
            organization = "com.google.googlejavaformat",
            name = "google-java-format"))
        .distinct,
      javafmt / includeFilter := "*.java")

  private def formatterVersionForCompatibleJavaVersion(compatibleJavaVersion: Int): String =
    compatibleJavaVersion match {
      case 11    => "1.24.0"
      case 17    => "1.28.0"
      case 21    => "1.35.0"
      case other =>
        throw new MessageOnlyException(
          s"Unsupported javafmtFormatterCompatibleJavaVersion: $other. Expected one of: 11, 17, 21.")
    }

  @transient
  private val javafmtDoFormatOnCompile =
    taskKey[Unit]("Format Java source files if javafmtOnCompile is on.")

}
