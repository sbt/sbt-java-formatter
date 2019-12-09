/*
 * Copyright 2015 Lightbend Inc.
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

package com.lightbend.sbt.javaformatter

import com.google.googlejavaformat.java.Formatter
import sbt._
import sbt.Keys._
import SbtCompat.{ Analysis, FileFunction }

import scala.collection.immutable.Seq

object JavaFormatter {

  def apply(
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      ref: ProjectRef,
      configuration: Configuration,
      streams: TaskStreams): Seq[File] = {

    val formatter = new Formatter()

    def log(label: String, logger: Logger)(message: String)(count: String) =
      logger.info(message.format(count, label))

    def performFormat(files: Set[File]) = {
      for (file <- files if file.exists) {
        try {
          val contents = IO.read(file)
          val formatted = formatter.formatSource(contents)
          if (formatted != contents) IO.write(file, formatted)
        } catch {
          // TODO what type of exceptions can we get here?
          case e: Exception =>
            streams.log.warn(s"Java Formatter error for $file: ${e.getMessage}")
        }
      }
    }

    // TODO figure out how to make changes to the settings trigger a reformat
    // Tried adding a file that the settings where written to, and include that in the cache,
    // but it still didn't trigger a reformat after reload. BTW, it doesn't work for Scalariform either.
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get.toSet
    val cache = streams.cacheDirectory / "java-formatter"
    val logFun = log("%s(%s)".format(Reference.display(ref), configuration), streams.log) _
    handleFiles(files, cache, logFun("Formatting %s %s ..."), performFormat)
    handleFiles(files, cache, logFun("Reformatted %s %s."), _ => ()).toList // recalculate cache because we're formatting in-place
  }

  def handleFiles(files: Set[File], cache: File, logFun: String => Unit, updateFun: Set[File] => Unit): Set[File] = {

    def handleUpdate(in: ChangeReport[File], out: ChangeReport[File]) = {
      val files = in.modified -- in.removed
      Analysis.counted("Java source", "", "s", files.size).foreach(logFun)
      updateFun(files)
      files
    }

    FileFunction.cached(cache)(FilesInfo.hash, FilesInfo.exists)(handleUpdate)(files)
  }
}
