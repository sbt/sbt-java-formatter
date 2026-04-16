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
import java.util.concurrent.atomic.AtomicBoolean

import _root_.sbt.Keys._
import _root_.sbt._
import _root_.sbt.util.CacheImplicits._
import _root_.sbt.util.{ CacheStoreFactory, FileInfo, Logger }
import com.google.googlejavaformat.java.JavaFormatterOptions
import scala.collection.immutable.Seq
import scala.sys.process.{ Process, ProcessLogger }

object JavaFormatter {

  private val GoogleJavaFormatMain = "com.google.googlejavaformat.java.Main"
  private val JavaHomeEnvVar = "SBT_JAVAFMT_JAVA_HOME"
  private val JavaHomeProperty = "sbt-javafmt.java.home"
  private val incompatibleJavaRuntimeHelpLogged = new AtomicBoolean(false)

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
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(
      cacheStoreFactory,
      files,
      streams.log,
      options,
      formatterClasspath,
      javaMaxHeap,
      fixImportsOnly = false,
      sortImports,
      removeUnusedImports,
      reflowLongStrings)
  }

  def fixImports(
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(
      cacheStoreFactory,
      files,
      streams.log,
      options,
      formatterClasspath,
      javaMaxHeap,
      fixImportsOnly = true,
      sortImports,
      removeUnusedImports,
      reflowLongStrings)
  }

  def check(
      baseDir: File,
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis =
      cachedCheckSources(
        cacheStoreFactory,
        baseDir,
        files,
        streams.log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly = false,
        sortImports,
        removeUnusedImports,
        reflowLongStrings)
    trueOrBoom(analysis)
  }

  def fixImportsCheck(
      baseDir: File,
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis =
      cachedCheckSources(
        cacheStoreFactory,
        baseDir,
        files,
        streams.log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly = true,
        sortImports,
        removeUnusedImports,
        reflowLongStrings)
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
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Analysis = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToCheck: Set[File] = updatedOrAdded
      val prevFailed: Set[File] = prev.failedCheck & outDiff.unmodified
      prevFailed.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
      val result = checkSources(
        baseDir,
        filesToCheck.toList,
        log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings)
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
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Analysis = {
    if (sources.nonEmpty) {
      log.info(s"Checking ${sources.size} Java source${plural(sources.size)}...")
    }
    val unformatted =
      runCheck(
        baseDir,
        sources,
        log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings)
    unformatted.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
    Analysis(failedCheck = unformatted)
  }

  private def cachedFormatSources(
      cacheStoreFactory: CacheStoreFactory,
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Unit = {
    trackSourcesViaCache(cacheStoreFactory, sources) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToFormat: Set[File] = updatedOrAdded | prev.failedCheck
      if (filesToFormat.nonEmpty) {
        log.info(s"Formatting ${filesToFormat.size} Java source${plural(filesToFormat.size)}...")
        formatSources(
          filesToFormat,
          log,
          options,
          formatterClasspath,
          javaMaxHeap,
          fixImportsOnly,
          sortImports,
          removeUnusedImports,
          reflowLongStrings)
      }
      Analysis(Set.empty)
    }
  }

  private def formatSources(
      sources: Set[File],
      log: Logger,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Unit = {
    val changed =
      runCheck(
        baseDir = new File("."),
        sources.toList,
        log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        warnOnFailure = false)
    if (changed.nonEmpty) {
      runReplace(
        changed.toList,
        log,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings)
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

  private def cliFlags(
      options: JavaFormatterOptions,
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Seq[String] = {
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
    val fixImportsOnlyFlags =
      if (fixImportsOnly) Seq("--fix-imports-only")
      else Nil
    val sortImportsFlags =
      if (sortImports) Nil
      else Seq("--skip-sorting-imports")
    val removeUnusedImportsFlags =
      if (removeUnusedImports) Nil
      else Seq("--skip-removing-unused-imports")
    val reflowLongStringsFlags =
      if (reflowLongStrings) Nil
      else Seq("--skip-reflowing-long-strings")
    styleFlags ++ javadocFlags ++ fixImportsOnlyFlags ++ sortImportsFlags ++ removeUnusedImportsFlags ++ reflowLongStringsFlags
  }

  private case class CliResult(exitCode: Int, stdout: Vector[String], stderr: Vector[String])

  private def logCliFailure(result: CliResult, log: Logger): Unit = {
    result.stderr.foreach(line => log.error(line))
    result.stdout.foreach(line => log.error(line))
    incompatibleJavaRuntimeHelp(result).foreach { message =>
      if (incompatibleJavaRuntimeHelpLogged.compareAndSet(false, true)) {
        log.info(message)
      }
    }
  }

  private def incompatibleJavaRuntimeHelp(result: CliResult): Option[String] = {
    val output = (result.stderr ++ result.stdout).mkString("\n")

    val unsupportedClassVersion =
      output.contains("UnsupportedClassVersionError") ||
      output.contains("compiled by a more recent version of the Java Runtime")

    val missingNewerJavacClass =
      output.contains("NoClassDefFoundError: com/sun/tools/javac/tree/JCTree$JCAnyPattern") ||
      output.contains("ClassNotFoundException: com.sun.tools.javac.tree.JCTree$JCAnyPattern")

    val olderFormatterOnNewerJdk =
      output.contains("NoSuchMethodError") &&
      (output.contains("com.sun.tools.javac.") || output.contains("jdk.compiler"))

    if (unsupportedClassVersion || missingNewerJavacClass) {
      Some(
        s"\n\n\nThe forked google-java-format JVM appears to be running on an incompatible Java version. " +
        s"Either set the $JavaHomeEnvVar environment variable or -D$JavaHomeProperty=... to point the formatter to a compatible JDK, " +
        s"or lower the sbt setting ThisBuild / javafmtFormatterCompatibleJavaVersion to match the available Java runtime.\n\n\n")
    } else if (olderFormatterOnNewerJdk) {
      Some(
        s"\n\n\nThe selected google-java-format runtime appears to be too old for the Java version used to launch the formatter JVM. " +
        s"Try increasing the sbt setting ThisBuild / javafmtFormatterCompatibleJavaVersion, " +
        s"or point the formatter to an older compatible JDK via the $JavaHomeEnvVar environment variable or -D$JavaHomeProperty=....\n\n\n")
    } else {
      None
    }
  }

  private lazy val javaBin: String = {
    val javaHomeSourceAndPath =
      sys.props
        .get(JavaHomeProperty)
        .filter(_.nonEmpty)
        .map(path => (JavaHomeProperty, path))
        .orElse(sys.env.get(JavaHomeEnvVar).filter(_.nonEmpty).map(path => (JavaHomeEnvVar, path)))
        .getOrElse(("java.home", sys.props("java.home")))
    val (javaHomeSource, javaHomePath) = javaHomeSourceAndPath
    val javaHome = new File(javaHomePath)
    val unixJava = new File(javaHome, "bin/java")
    val windowsJava = new File(javaHome, "bin/java.exe")
    val javaExec =
      if (unixJava.isFile) unixJava
      else if (windowsJava.isFile) windowsJava
      else {
        throw new MessageOnlyException(
          s"Could not locate a Java launcher under ${javaHomeSource}=${javaHome.getAbsolutePath}")
      }
    javaExec.getAbsolutePath
  }

  private def javaArgs(args: Seq[String], formatterClasspath: Seq[File], javaMaxHeap: Option[String]): Seq[String] = {
    val formatterClasspathString = formatterClasspath.map(_.getAbsolutePath).distinct.mkString(File.pathSeparator)
    javaMaxHeap.toList
      .map(heap => s"-Xmx$heap") ++ JavaExports ++ Seq("-cp", formatterClasspathString, GoogleJavaFormatMain) ++ args
  }

  private def renderJavaArg(arg: String): String =
    if (arg.isEmpty || arg.exists(_.isWhitespace) || arg.contains("\"")) {
      "\"" + arg.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
    } else {
      arg
    }

  private def runCli(
      args: Seq[String],
      formatterClasspath: Seq[File],
      log: Logger,
      javaMaxHeap: Option[String]): CliResult =
    IO.withTemporaryFile("google-java-format-java", ".args") { argFile =>
      IO.writeLines(argFile, javaArgs(args, formatterClasspath, javaMaxHeap).map(renderJavaArg))
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
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      warnOnFailure: Boolean = true): Set[File] = {
    if (sources.isEmpty) {
      return Set.empty
    }
    val args =
      cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings) ++ Seq(
        "--dry-run",
        "--set-exit-if-changed") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, formatterClasspath, log, javaMaxHeap)
    val changed = result.stdout.iterator.map(path => file(path)).toSet
    result.exitCode match {
      case 0 | 1 =>
        if (result.exitCode == 1 && changed.isEmpty) {
          logCliFailure(result, log)
          throw new MessageOnlyException("google-java-format check failed")
        }
        changed
      case _ =>
        if (warnOnFailure) {
          logCliFailure(result, log)
        }
        throw new MessageOnlyException("google-java-format check failed")
    }
  }

  private def runReplace(
      sources: Seq[File],
      log: Logger,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean): Unit = {
    if (sources.isEmpty) {
      return
    }
    val args =
      cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings) ++ Seq(
        "--replace") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, formatterClasspath, log, javaMaxHeap)
    if (result.exitCode != 0) {
      result.stderr.foreach(line => log.error(line))
      result.stdout.foreach(line => log.error(line))
      throw new MessageOnlyException("google-java-format failed")
    }
  }

}
