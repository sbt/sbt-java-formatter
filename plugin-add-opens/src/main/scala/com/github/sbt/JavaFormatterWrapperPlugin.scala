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

import scala.sys.process.Process

import sbt._
import sbt.Keys._

//import com.github.sbt.JavaFormatterPlugin

object JavaFormatterWrapperPlugin extends AutoPlugin {
  override def requires = JavaFormatterPlugin

  override def trigger = allRequirements

  override def globalSettings =
    Seq(commands += javafmt, commands += javafmtCheck, commands += javafmtAll, commands += javafmtCheckAll)

  private val javafmtWrapperProp = "play.javafmt.wrapper"

  private val javafmtExports =
    Seq("api", "code", "file", "parser", "tree", "util").map { exportedPackage =>
      s"-J--add-opens=jdk.compiler/com.sun.tools.javac.${exportedPackage}=ALL-UNNAMED"
    }

  private def javafmtCommand(name: String, delegatedCommand: String): Command =
    Command.command(
      name,
      Help.more(
        name,
        s"Runs $delegatedCommand in a fresh sbt JVM with the required jdk.compiler module-opening flags")) { state =>
      if (sys.props.get(javafmtWrapperProp).contains("true")) {
        delegatedCommand :: state
      } else {
        val extracted = Project.extract(state)
        val base = extracted.get(ThisBuild / baseDirectory)
        val sbtArgs = Seq("sbt", "--server", s"-D$javafmtWrapperProp=true") ++ javafmtExports ++ Seq(name)
        val exitCode = Process(sbtArgs, base).!
        if (exitCode == 0) state else state.fail
      }
    }

  private val javafmt = javafmtCommand("javafmt", "javafmt")
  private val javafmtCheck = javafmtCommand("javafmtCheck", "javafmtCheck")
  private val javafmtAll = javafmtCommand("javafmtAll", "all javafmtAll")
  private val javafmtCheckAll = javafmtCommand("javafmtCheckAll", "all javafmtCheckAll")
}
