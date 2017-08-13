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

package com.lightbend.sbt.javaformatter

import org.eclipse.jdt.core.formatter.CodeFormatter
import org.eclipse.jdt.core.{ ToolFactory, JavaCore }
import org.eclipse.jface.text.Document
import org.eclipse.text.edits.TextEdit
import sbt._
import sbt.Keys._
import scala.collection.immutable.Seq

object JavaFormatter {
  // TODO configurable
  private val LineSeparator = System.getProperty("line.separator")

  object JavaFormatterSettings {
    val defaults = Map[String, String](
      JavaCore.COMPILER_SOURCE -> "1.8",
      JavaCore.COMPILER_COMPLIANCE -> "1.8",
      JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM -> "1.8")
  }

  class JavaFormatterSettings(
    settingsMap:   Map[String, String],
    sourceVersion: Option[String],
    targetVersion: Option[String]) {
    import JavaFormatterSettings._

    val settings: Map[String, String] = {
      // override settings in the map if these are set
      val merged = List(
        JavaCore.COMPILER_SOURCE -> sourceVersion,
        JavaCore.COMPILER_COMPLIANCE -> sourceVersion,
        JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM -> targetVersion).foldLeft(settingsMap) {
          case (r1, (key, value)) => value.foldLeft(r1) { case (r2, v) => r2.updated(key, v) }
        }
      // add defaults unless they are set in the map
      val withDefaults = defaults.foldLeft(merged) {
        case (r, (key, value)) => r.updated(key, r.getOrElse(key, value))
      }
      withDefaults
    }
  }

  def apply(
    sourceDirectories: Seq[File],
    includeFilter:     FileFilter,
    excludeFilter:     FileFilter,
    ref:               ProjectRef,
    configuration:     Configuration,
    streams:           TaskStreams,
    settings:          JavaFormatterSettings): Seq[File] = {

    import scala.collection.JavaConverters._
    val formatter = ToolFactory.createCodeFormatter(settings.settings.asJava)

    def log(label: String, logger: Logger)(message: String)(count: String) =
      logger.info(message.format(count, label))

    def performFormat(files: Set[File]) = {
      for (file <- files if file.exists) {
        try {
          val contents = IO.read(file)
          val te: TextEdit = formatter.format(CodeFormatter.K_COMPILATION_UNIT, contents, 0, contents.length(), 0, LineSeparator)
          val formatted = if (te == null) {
            // TODO a bit more informative maybe?
            streams.log.warn(s"Java Formatter can't create formatter for $file")
            contents
          } else {
            val doc = new Document(contents)
            te.apply(doc)
            doc.get()
          }
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

  def handleFiles(
    files:     Set[File],
    cache:     File,
    logFun:    String => Unit,
    updateFun: Set[File] => Unit): Set[File] = {

    def handleUpdate(in: ChangeReport[File], out: ChangeReport[File]) = {
      val files = in.modified -- in.removed
      internal.inc.Analysis.counted("Java source", "", "s", files.size) foreach logFun
      updateFun(files)
      files
    }

    FileFunction.cached(util.CacheStoreFactory(cache), FilesInfo.hash, FilesInfo.exists)(handleUpdate)(files)
  }
}
