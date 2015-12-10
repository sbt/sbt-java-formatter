/*
 * Copyright 2015 Typesafe Inc.
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

package com.typesafe.sbt

import com.typesafe.sbt.javaformatter.JavaFormatter
import com.typesafe.sbt.javaformatter.JavaFormatter.JavaFormatterSettings
import sbt._
import sbt.Keys._

object AutomateJavaFormatterPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = automateFor(Compile, Test)

  def automateFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(compile := compile.dependsOn(JavaFormatterPlugin.JavaFormatterKeys.format).value)
  }
}

object JavaFormatterPlugin extends AutoPlugin {

  object JavaFormatterKeys {
    val format: TaskKey[Seq[File]] =
      TaskKey("java-formatter-format", "Format (Java) sources using the eclipse formatter")
    val settings: SettingKey[Map[String, String]] =
      SettingKey("java-formatter-settings", "A Map of eclipse formatter settings and values")
    val sourceLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-source-level", "Java source level. Overrides source level defined in settings.")
    val targetLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-target-level", "Java target level. Overrides target level defined in settings.")
  }

  val autoImport = JavaFormatterKeys

  import autoImport._

  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = settingsFor(Compile, Test) ++ notToBeScopedSettings

  def settingsFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(toBeScopedSettings)
  }

  private def setOrNone(value: String): Option[String] = if (value == "default") None else Some(value)

  def settingsFromProfile(file: File): Map[String, String] = {
    val xml = scala.xml.XML.loadFile(file)
    (xml \\ "setting").foldLeft(Map.empty[String, String]) {
      case (r, node) => r.updated((node \ "@id").text, (node \ "@value").text)
    }
  }

  def toBeScopedSettings: Seq[Setting[_]] =
    List(
      (sourceDirectories in format) := List(javaSource.value),
      format := {
        val formatterSettings = new JavaFormatterSettings(settings.value, sourceLevel.value, targetLevel.value)
        JavaFormatter(
          (sourceDirectories in format).value.toList,
          (includeFilter in format).value,
          (excludeFilter in format).value,
          thisProjectRef.value,
          configuration.value,
          streams.value,
          formatterSettings)
      }
    )

  def notToBeScopedSettings: Seq[Setting[_]] =
    List(
      includeFilter in format := "*.java",
      settings := Map.empty,
      sourceLevel := None,
      targetLevel := None
    )
}
