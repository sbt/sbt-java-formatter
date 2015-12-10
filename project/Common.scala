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

import sbt._
import sbt.Keys._
import de.heikoseeberger.sbtheader.{ HeaderPlugin, AutomateHeaderPlugin }
import de.heikoseeberger.sbtheader.license.Apache2_0
import com.typesafe.sbt.SbtScalariform.{ scalariformSettings, ScalariformKeys }
import scalariform.formatter.preferences._

/**
 * Common sbt settings — automatically added to all projects.
 */
object Common extends AutoPlugin {

  override def trigger  = allRequirements

  override def requires = plugins.JvmPlugin && HeaderPlugin

  // AutomateHeaderPlugin is not an allRequirements-AutoPlugin, so explicitly add settings here:
  override def projectSettings = scalariformSettings ++ AutomateHeaderPlugin.projectSettings ++ Seq(
    organization := "com.typesafe.sbt",
    scalacOptions ++= Seq("-encoding", "UTF-8", "-target:jvm-1.6", "-unchecked", "-deprecation", "-feature"),
    javacOptions ++= Seq("-encoding", "UTF-8", "-source", "1.6", "-target", "1.6"),

    // Scalariform settings
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 100)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(PreserveDanglingCloseParenthesis, true)
      .setPreference(AlignParameters, true),

    // Header settings
    HeaderPlugin.autoImport.headers := Map(
      "scala" -> Apache2_0("2015", "Typesafe Inc."),
      "java" ->  Apache2_0("2015", "Typesafe Inc."),
      "conf" ->  Apache2_0("2015", "Typesafe Inc.", "#")
    )
  )
}
