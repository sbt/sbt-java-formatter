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

package com.github.sbt.javaformatter

import _root_.sbt.Keys._
import _root_.sbt._
import _root_.sbt.util.CacheImplicits._
import _root_.sbt.util.{ CacheStoreFactory, FileInfo, Logger }
import com.google.googlejavaformat.java.{
  Formatter => GoogleFormatter,
  JavaFormatterOptions => GoogleJavaFormatterOptions
}
import com.palantir.javaformat.java.{
  Formatter => PalantirFormatter,
  JavaFormatterOptions => PalantirJavaFormatterOptions
}

import scala.collection.immutable.Seq

object JavaFormatter {

  def apply(
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      formatter: Formatter,
      options: GoogleJavaFormatterOptions,
      palantirOptions: PalantirJavaFormatterOptions): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(cacheStoreFactory, files, streams.log)(
      formatter,
      new GoogleFormatter(options),
      PalantirFormatter.createFormatter(palantirOptions))
  }

  def check(
      baseDir: File,
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      formatter: Formatter,
      options: GoogleJavaFormatterOptions,
      palantirOptions: PalantirJavaFormatterOptions): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis =
      cachedCheckSources(cacheStoreFactory, baseDir, files, streams.log)(
        formatter,
        new GoogleFormatter(options),
        PalantirFormatter.createFormatter(palantirOptions))
    trueOrBoom(analysis)
  }

  private def plural(i: Int) = if (i == 1) "" else "s"

  private def trueOrBoom(analysis: Analysis): Boolean = {
    val failureCount = analysis.failedCheck.size
    if (failureCount > 0) {
      throw new MessageOnlyException(s"${failureCount} file${plural(failureCount)} must be formatted")
    }
    true
  }

  case class Analysis(failedCheck: Set[File])

  object Analysis {

    import sjsonnew.{ :*:, LList, LNil }

    implicit val analysisIso: sjsonnew.IsoLList.Aux[Analysis, Set[File] :*: LNil] = LList.iso(
      { (a: Analysis) => ("failedCheck", a.failedCheck) :*: LNil },
      { (in: Set[File] :*: LNil) =>
        Analysis(in.head)
      })
  }

  private def cachedCheckSources(cacheStoreFactory: CacheStoreFactory, baseDir: File, sources: Seq[File], log: Logger)(
      implicit
      formatter: Formatter,
      googleFormatter: GoogleFormatter,
      palantirFormatter: PalantirFormatter): Analysis = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToCheck: Set[File] = updatedOrAdded
      val prevFailed: Set[File] = prev.failedCheck & outDiff.unmodified
      prevFailed.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
      val result = checkSources(baseDir, filesToCheck.toList, log)
      prev.copy(failedCheck = result.failedCheck | prevFailed)
    }
  }

  private def warnBadFormat(file: File, log: Logger): Unit = {
    log.warn(s"${file.toString} isn't formatted properly!")
  }

  private def checkSources(baseDir: File, sources: Seq[File], log: Logger)(implicit
      formatter: Formatter,
      googleFormatter: GoogleFormatter,
      palantirFormatter: PalantirFormatter): Analysis = {
    if (sources.nonEmpty) {
      log.info(s"Checking ${sources.size} Java source${plural(sources.size)}...")
    }
    val unformatted = withFormattedSources(sources, log)((file, input, output) => {
      val diff = input != output
      if (diff) {
        warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log)
        Some(file)
      } else None
    }).flatten.flatten.toSet
    Analysis(failedCheck = unformatted)
  }

  private def cachedFormatSources(cacheStoreFactory: CacheStoreFactory, sources: Seq[File], log: Logger)(implicit
      formatter: Formatter,
      googleFormatter: GoogleFormatter,
      palantirFormatter: PalantirFormatter): Unit = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToFormat: Set[File] = updatedOrAdded | prev.failedCheck
      if (filesToFormat.nonEmpty) {
        log.info(s"Formatting ${filesToFormat.size} Java source${plural(filesToFormat.size)}...")
        formatSources(filesToFormat, log)
      }
      Analysis(Set.empty)
    }
  }

  private def formatSources(sources: Set[File], log: Logger)(implicit
      formatter: Formatter,
      googleFormatter: GoogleFormatter,
      palantirFormatter: PalantirFormatter): Unit = {
    val cnt = withFormattedSources(sources.toList, log)((file, input, output) => {
      if (input != output) {
        IO.write(file, output)
        1
      } else {
        0
      }
    }).flatten.sum
    log.info(s"Reformatted $cnt Java source${plural(cnt)}")
  }

  private def trackSourcesViaCache(cacheStoreFactory: CacheStoreFactory, sources: Seq[File])(
      f: (ChangeReport[File], Analysis) => Analysis): Analysis = {
    val prevTracker = Tracked.lastOutput[Unit, Analysis](cacheStoreFactory.make("last")) { (_, prev0) =>
      val prev = prev0.getOrElse(Analysis(Set.empty))
      Tracked.diffOutputs(cacheStoreFactory.make("output-diff"), FileInfo.lastModified)(sources.toSet) {
        (outDiff: ChangeReport[File]) => f(outDiff, prev)
      }

    }
    prevTracker(())
  }

  private def withFormattedSources[T](sources: Seq[File], log: Logger)(onFormat: (File, String, String) => T)(implicit
      formatter: Formatter,
      googleFormatter: GoogleFormatter,
      palantirFormatter: PalantirFormatter): Seq[Option[T]] = {
    sources.map { file =>
      val input = IO.read(file)
      try {
        val output = formatter match {
          case Formatter.PALANTIR => palantirFormatter.formatSourceAndFixImports(input)
          case _                  => googleFormatter.formatSourceAndFixImports(input)
        }
        Some(onFormat(file, input, output))
      } catch {
        case e: Exception => Some(onFormat(file, input, input))
      }
    }
  }

}
