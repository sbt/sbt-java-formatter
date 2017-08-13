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

import sbt._
import sbt.Keys._
import bintray.BintrayKeys._

/**
 * Publish to private bintray repository.
 */
object Publish extends AutoPlugin {

  override def trigger = allRequirements

  override def requires = plugins.JvmPlugin

  override def projectSettings = Seq(
    description := "Formats Java code in your project using the Eclipse formatter.",
    licenses += ("Apache-2.0", url("https://www.apache.org/licenses/LICENSE-2.0.html")),
    publishMavenStyle := false,
    bintrayRepository := "sbt-plugins",
    bintrayOrganization := None
  )

}

/**
 * For projects that are not published.
 */
object NoPublish extends AutoPlugin {

  override def requires = plugins.JvmPlugin && Publish

  override def projectSettings = Seq(
    publish := (),
    publishLocal := ()
  )

}
