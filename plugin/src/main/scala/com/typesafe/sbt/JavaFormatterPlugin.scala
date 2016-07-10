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
    _ ++ inConfig(_)(compile := compile.dependsOn(JavaFormatterPlugin.JavaFormatterKeys.formatJava).value)
  }
}

object JavaFormatterPlugin extends AutoPlugin {

  val JavaFormatterConfig = config("java-formatter")

  object JavaFormatterKeys {
    val formatJava: TaskKey[Seq[File]] =
      TaskKey("java-formatter-format", "Format (Java) sources using the eclipse formatter")
    val settings: SettingKey[Map[String, String]] =
      SettingKey("java-formatter-settings", "A Map of eclipse formatter settings and values")
    val sourceLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-source-level", "Java source level. Overrides source level defined in settings.")
    val targetLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-target-level", "Java target level. Overrides target level defined in settings.")
    val javaFormattingSettingsFile: SettingKey[Option[File]] =
      SettingKey("javaFormattingSettingsFile", "XML file with eclipse formatter settings.")
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
      (sourceDirectories in formatJava) := List(javaSource.value),
      formatJava := {
        val s = (settings in JavaFormatterConfig).value
        val sourceLv = (sourceLevel in JavaFormatterConfig).value
        val targetLv = (targetLevel in JavaFormatterConfig).value
        val formatterSettings = new JavaFormatterSettings(s, sourceLv, targetLv)
        JavaFormatter(
          (sourceDirectories in formatJava).value.toList,
          (includeFilter in formatJava).value,
          (excludeFilter in formatJava).value,
          thisProjectRef.value,
          configuration.value,
          streams.value,
          formatterSettings)
      }
    )

  def notToBeScopedSettings: Seq[Setting[_]] =
    List(
      includeFilter in formatJava := "*.java",
      sourceLevel in JavaFormatterConfig := None,
      targetLevel in JavaFormatterConfig := None,
      javaFormattingSettingsFile in JavaFormatterConfig := None,
      javaFormattingSettingsFile := (javaFormattingSettingsFile in JavaFormatterConfig).value,
      settings in JavaFormatterConfig := {
        javaFormattingSettingsFile.value match {
          case Some(settingsXml) =>
            settingsFromProfile(settingsXml)
          case None =>
            // can't depend on `streams` in a setting here
            System.err.println("Define `javaFormattingSettingsFile` to configure the Java formatter.")
            Map.empty
        }
      }
    )
}
