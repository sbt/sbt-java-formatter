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

  object autoImport {
    val javafmt: TaskKey[Unit] = taskKey("Format Java sources")
    val javafmtCheck: TaskKey[Boolean] = taskKey("Fail, if a Java source needs reformatting.")
    val javafmtAll: TaskKey[Unit] = taskKey(
      "Execute the javafmt task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    val javafmtCheckAll: TaskKey[Unit] = taskKey(
      "Execute the javafmtCheck task for all configurations in which it is enabled. " +
      "(By default this means the Compile and Test configurations.)")
    val javafmtOnCompile = settingKey[Boolean]("Format Java source files on compile, off by default.")
    val javafmtStyle =
      settingKey[JavaFormatterOptions.Style]("Define formatting style, Google Java Style (default) or AOSP")
    val javafmtOptions = settingKey[JavaFormatterOptions](
      "Define all formatting options such as style or enabling Javadoc formatting. See _JavaFormatterOptions_ for more")
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
      })
  }

  override def globalSettings: Seq[Def.Setting[?]] =
    Seq(javafmtOnCompile := false, javafmtStyle := JavaFormatterOptions.Style.GOOGLE)

  def toBeScopedSettings: Seq[Setting[?]] =
    List(
      (javafmt / sourceDirectories) := List(javaSource.value),
      javafmtOptions := JavaFormatterOptions.builder().style(javafmtStyle.value).build(),
      javafmt := {
        val streamz = streams.value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = streamz.cacheStoreFactory
        val options = javafmtOptions.value
        JavaFormatter(sD, iF, eF, streamz, cache, options)
      },
      javafmtCheck := {
        val streamz = streams.value
        val baseDir = (ThisBuild / baseDirectory).value
        val sD = (javafmt / sourceDirectories).value.toList
        val iF = (javafmt / includeFilter).value
        val eF = (javafmt / excludeFilter).value
        val cache = (javafmt / streams).value.cacheStoreFactory
        val options = javafmtOptions.value
        JavaFormatter.check(baseDir, sD, iF, eF, streamz, cache, options)
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
    List(javafmt / includeFilter := "*.java")

  private val javafmtDoFormatOnCompile =
    taskKey[Unit]("Format Java source files if javafmtOnCompile is on.")

}
