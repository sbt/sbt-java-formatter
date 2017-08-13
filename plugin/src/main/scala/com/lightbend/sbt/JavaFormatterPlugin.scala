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

package com.lightbend.sbt

import com.lightbend.sbt.javaformatter.JavaFormatter
import com.lightbend.sbt.javaformatter.JavaFormatter.JavaFormatterSettings
import sbt._
import sbt.Keys._

import scala.annotation.tailrec

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
    val javaFormattingSettingsFilename: SettingKey[String] =
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
        val streamz = streams.value
        val log = streamz.log
        val sourceLv = sourceLevel.value
        val targetLv = targetLevel.value
        val s = settings.value
        val sD = (sourceDirectories in format).value.toList
        val iF = (includeFilter in format).value
        val eF = (excludeFilter in format).value
        val tPR = thisProjectRef.value
        val c = configuration.value
        if (s.isEmpty) {
          log.warn("Unable to locate formatting preferences, skipping formatting of Java sources!")
          Seq.empty
        } else {
          log.info("")
          val formatterSettings = new JavaFormatterSettings(s, sourceLv, targetLv)
          JavaFormatter(
            sD,
            iF,
            eF,
            tPR,
            c,
            streamz,
            formatterSettings)
        }
      }
    )

  def notToBeScopedSettings: Seq[Setting[_]] =
    List(
      includeFilter in format := "*.java",
      sourceLevel := None,
      targetLevel := None,
      // Don't parse the XML file for every single project over and over again!
      javaFormattingSettingsFilename := "formatting-java.xml",
      settings := {
        val filename = javaFormattingSettingsFilename.value
        val settingsFile = locateFormattingXml((baseDirectory in ThisBuild).value, baseDirectory.value, filename)

        settingsFile match {
          case Some(s) => settingsFromProfile(s)
          case None =>
            System.err.println(s"Configured `javaFormattingSettingsFilename` (${baseDirectory.value / filename}) does not exist! Unable to format Java code.")
            Map.empty
        }
      }
    )

  @tailrec final def locateFormattingXml(max: File, current: File, name: String): Option[File] = {
    val now = current / name
    val `now/project` = current / "project" / name
    if (`now/project`.exists) Some(`now/project`)
    else if (now.exists) Some(now)
    else if (current == max) None
    else locateFormattingXml(max, new File(current, ".."), name)
  }

}
