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

import java.io.File
import java.net.URLClassLoader

import _root_.sbt.Keys._
import _root_.sbt._
import _root_.sbt.util.CacheImplicits._
import _root_.sbt.util.{ CacheStoreFactory, FileInfo, Logger }
import com.google.googlejavaformat.java.JavaFormatterOptions
import scala.collection.immutable.Seq
import scala.sys.process.{ Process, ProcessLogger }

object JavaFormatter {

  private val GoogleJavaFormatMain = "com.google.googlejavaformat.java.Main"

  private val JavaExports = Seq("api", "code", "file", "parser", "tree", "util").map { exportedPackage =>
    s"--add-exports=jdk.compiler/com.sun.tools.javac.$exportedPackage=ALL-UNNAMED"
  }

  def apply(
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(cacheStoreFactory, files, streams.log, options, javaMaxHeap)
  }

  def check(
      baseDir: File,
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis = cachedCheckSources(cacheStoreFactory, baseDir, files, streams.log, options, javaMaxHeap)
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

  private def cachedCheckSources(
      cacheStoreFactory: CacheStoreFactory,
      baseDir: File,
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Analysis = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToCheck: Set[File] = updatedOrAdded
      val prevFailed: Set[File] = prev.failedCheck & outDiff.unmodified
      prevFailed.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
      val result = checkSources(baseDir, filesToCheck.toList, log, options, javaMaxHeap)
      prev.copy(failedCheck = result.failedCheck | prevFailed)
    }
  }

  private def warnBadFormat(file: File, log: Logger): Unit = {
    log.warn(s"${file.toString} isn't formatted properly!")
  }

  private def checkSources(
      baseDir: File,
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Analysis = {
    if (sources.nonEmpty) {
      log.info(s"Checking ${sources.size} Java source${plural(sources.size)}...")
    }
    val unformatted = runCheck(baseDir, sources, log, options, javaMaxHeap)
    unformatted.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
    Analysis(failedCheck = unformatted)
  }

  private def cachedFormatSources(
      cacheStoreFactory: CacheStoreFactory,
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Unit = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToFormat: Set[File] = updatedOrAdded | prev.failedCheck
      if (filesToFormat.nonEmpty) {
        log.info(s"Formatting ${filesToFormat.size} Java source${plural(filesToFormat.size)}...")
        formatSources(filesToFormat, log, options, javaMaxHeap)
      }
      Analysis(Set.empty)
    }
  }

  private def formatSources(
      sources: Set[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Unit = {
    val changed =
      runCheck(baseDir = new File("."), sources.toList, log, options, javaMaxHeap, warnOnFailure = false)
    if (changed.nonEmpty) {
      runReplace(changed.toList, log, options, javaMaxHeap)
    }
    val cnt = changed.size
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

  private def cliFlags(options: JavaFormatterOptions): Seq[String] = {
    if (!options.reorderModifiers()) {
      throw new MessageOnlyException(
        "The forked google-java-format CLI does not support reorderModifiers = false. " +
        "Please use the default reorderModifiers setting.")
    }
    val styleFlags =
      if (options.style() == JavaFormatterOptions.Style.AOSP) Seq("--aosp")
      else Nil
    val javadocFlags =
      if (options.formatJavadoc()) Nil
      else Seq("--skip-javadoc-formatting")
    styleFlags ++ javadocFlags
  }

  private case class CliResult(exitCode: Int, stdout: Vector[String], stderr: Vector[String])

  private def classpathFrom(loader: ClassLoader): List[String] =
    loader match {
      case null                      => Nil
      case urlLoader: URLClassLoader =>
        urlLoader.getURLs.iterator.map(url => new File(url.toURI).getAbsolutePath).toList ++ classpathFrom(
          loader.getParent)
      case _ =>
        classpathFrom(loader.getParent)
    }

  private lazy val formatterClasspath: String =
    classpathFrom(getClass.getClassLoader).distinct.mkString(File.pathSeparator)

  private lazy val javaBin: String = {
    val javaHome = new File(sys.props("java.home"))
    val unixJava = new File(javaHome, "bin/java")
    val windowsJava = new File(javaHome, "bin/java.exe")
    val javaExec =
      if (unixJava.isFile) unixJava
      else if (windowsJava.isFile) windowsJava
      else {
        throw new MessageOnlyException(s"Could not locate a Java launcher under java.home=${javaHome.getAbsolutePath}")
      }
    javaExec.getAbsolutePath
  }

  private def javaArgs(args: Seq[String], javaMaxHeap: Option[String]): Seq[String] =
    javaMaxHeap.toList
      .map(heap => s"-Xmx$heap") ++ JavaExports ++ Seq("-cp", formatterClasspath, GoogleJavaFormatMain) ++ args

  private def renderJavaArg(arg: String): String =
    if (arg.isEmpty || arg.exists(_.isWhitespace) || arg.contains("\"")) {
      "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    } else {
      arg
    }

  private def runCli(args: Seq[String], log: Logger, javaMaxHeap: Option[String]): CliResult =
    IO.withTemporaryFile("google-java-format-java", ".args") { argFile =>
      IO.writeLines(argFile, javaArgs(args, javaMaxHeap).map(renderJavaArg))
      val stdout = Vector.newBuilder[String]
      val stderr = Vector.newBuilder[String]
      val exitCode = Process(Seq(javaBin, s"@${argFile.getAbsolutePath}")).!(ProcessLogger(stdout += _, stderr += _))
      CliResult(exitCode, stdout.result(), stderr.result())
    }

  private def runCheck(
      baseDir: File,
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String],
      warnOnFailure: Boolean = true): Set[File] = {
    if (sources.isEmpty) {
      return Set.empty
    }
    val args = cliFlags(options) ++ Seq("--dry-run", "--set-exit-if-changed") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, log, javaMaxHeap)
    val changed = result.stdout.iterator.map(path => file(path)).toSet
    result.exitCode match {
      case 0 | 1 =>
        changed
      case _ =>
        if (warnOnFailure) {
          result.stderr.foreach(line => log.error(line))
          result.stdout.foreach(line => log.error(line))
        }
        throw new MessageOnlyException("google-java-format check failed")
    }
  }

  private def runReplace(
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      javaMaxHeap: Option[String]): Unit = {
    if (sources.isEmpty) {
      return
    }
    val args = cliFlags(options) ++ Seq("--replace") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, log, javaMaxHeap)
    if (result.exitCode != 0) {
      result.stderr.foreach(line => log.error(line))
      result.stdout.foreach(line => log.error(line))
      throw new MessageOnlyException("google-java-format failed")
    }
  }

}
