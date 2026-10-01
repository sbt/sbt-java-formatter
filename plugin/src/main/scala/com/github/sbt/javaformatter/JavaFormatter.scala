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

import java.io.{ File, IOException }
import java.nio.file.InvalidPathException

import _root_.sbt.Keys._
import _root_.sbt._
import _root_.sbt.util.CacheImplicits._
import _root_.sbt.util.{ CacheStoreFactory, FileInfo, Logger }
import com.google.googlejavaformat.java.JavaFormatterOptions
import scala.collection.immutable.Seq
import scala.sys.process.{ Process, ProcessLogger }

object JavaFormatter {

  private[sbt] val DefaultMaxLineLength = 100

  private val GoogleJavaFormatMain = "com.google.googlejavaformat.java.Main"
  private val JavaHomeEnvVar = "SBT_JAVAFMT_JAVA_HOME"
  private val JavaHomeProperty = "sbt-javafmt.java.home"
  private val incompatibleJavaRuntimeHelpLoggedByProject =
    new scala.collection.concurrent.TrieMap[String, String]

  private val JavaExports = Seq("api", "code", "file", "parser", "tree", "util").map { exportedPackage =>
    s"--add-exports=jdk.compiler/com.sun.tools.javac.$exportedPackage=ALL-UNNAMED"
  }

  def apply(
      projectId: String,
      invocationId: String,
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
      reflowLongStrings: Boolean,
      maxLineLength: Int): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(
      cacheStoreFactory,
      files,
      streams.log,
      projectId,
      invocationId,
      options,
      formatterClasspath,
      javaMaxHeap,
      fixImportsOnly = false,
      sortImports,
      removeUnusedImports,
      reflowLongStrings,
      maxLineLength)
  }

  def fixImports(
      projectId: String,
      invocationId: String,
      sourceDirectories: Seq[File],
      includeFilter: FileFilter,
      excludeFilter: FileFilter,
      streams: TaskStreams,
      cacheStoreFactory: CacheStoreFactory,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      sortImports: Boolean,
      removeUnusedImports: Boolean): Unit = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    cachedFormatSources(
      cacheStoreFactory,
      files,
      streams.log,
      projectId,
      invocationId,
      options,
      formatterClasspath,
      javaMaxHeap,
      fixImportsOnly = true,
      sortImports,
      removeUnusedImports,
      // Full-formatting options are ignored in import-only mode.
      reflowLongStrings = true,
      maxLineLength = DefaultMaxLineLength)
  }

  def check(
      projectId: String,
      invocationId: String,
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
      reflowLongStrings: Boolean,
      maxLineLength: Int): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis =
      cachedCheckSources(
        cacheStoreFactory,
        baseDir,
        files,
        streams.log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly = false,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        maxLineLength)
    trueOrBoom(analysis)
  }

  def fixImportsCheck(
      projectId: String,
      invocationId: String,
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
      removeUnusedImports: Boolean): Boolean = {
    val files = sourceDirectories.descendantsExcept(includeFilter, excludeFilter).get().toList
    val analysis =
      cachedCheckSources(
        cacheStoreFactory,
        baseDir,
        files,
        streams.log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly = true,
        sortImports,
        removeUnusedImports,
        // Full-formatting options are ignored in import-only mode.
        reflowLongStrings = true,
        maxLineLength = DefaultMaxLineLength)
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
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Analysis = {
    val flags = cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings, maxLineLength)
    val inputs = formatterCacheInputs(flags, formatterClasspath, javaMaxHeap)
    trackSourcesViaCache(cacheStoreFactory, sources, inputs, fixImportsOnly) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToCheck: Set[File] = updatedOrAdded
      val prevFailed: Set[File] = prev.failedCheck & outDiff.unmodified
      prevFailed.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
      val result = checkSources(
        baseDir,
        filesToCheck.toList,
        log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        maxLineLength)
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
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Analysis = {
    if (sources.nonEmpty) {
      log.info(s"Checking ${sources.size} Java source${plural(sources.size)}...")
    }
    val unformatted =
      runCheck(
        baseDir,
        sources,
        log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        maxLineLength)
    unformatted.foreach { file => warnBadFormat(file.relativeTo(baseDir).getOrElse(file), log) }
    Analysis(failedCheck = unformatted)
  }

  private def cachedFormatSources(
      cacheStoreFactory: CacheStoreFactory,
      sources: Seq[File],
      log: Logger,
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Unit = {
    val flags = cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings, maxLineLength)
    val inputs = formatterCacheInputs(flags, formatterClasspath, javaMaxHeap)
    trackSourcesViaCache(cacheStoreFactory, sources, inputs, fixImportsOnly) { (outDiff, prev) =>
      log.debug(outDiff.toString)
      val updatedOrAdded = outDiff.modified & outDiff.checked
      val filesToFormat: Set[File] = updatedOrAdded | prev.failedCheck
      if (filesToFormat.nonEmpty) {
        log.info(s"Formatting ${filesToFormat.size} Java source${plural(filesToFormat.size)}...")
        formatSources(
          filesToFormat,
          log,
          projectId,
          invocationId,
          options,
          formatterClasspath,
          javaMaxHeap,
          fixImportsOnly,
          sortImports,
          removeUnusedImports,
          reflowLongStrings,
          maxLineLength)
      }
      Analysis(Set.empty)
    }
  }

  private def formatSources(
      sources: Set[File],
      log: Logger,
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Unit = {
    val changed =
      runCheck(
        baseDir = new File("."),
        sources.toList,
        log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        maxLineLength,
        warnOnFailure = false)
    if (changed.nonEmpty) {
      runReplace(
        changed.toList,
        log,
        projectId,
        invocationId,
        options,
        formatterClasspath,
        javaMaxHeap,
        fixImportsOnly,
        sortImports,
        removeUnusedImports,
        reflowLongStrings,
        maxLineLength)
    }
    val cnt = changed.size
    log.info(s"Reformatted $cnt Java source${plural(cnt)}")
  }

  private def javaHomeCacheInput: String = {
    val javaHome = new File(javaHomeSourceAndPath._2)
    try {
      // Avoid Java 11's canonical-path cache so retargeted symlinks are noticed immediately.
      javaHome.toPath.toRealPath().toString
    } catch {
      // Missing or invalid homes must only fail when the formatter is actually launched.
      case _: IOException | _: InvalidPathException => javaHome.getAbsolutePath
    }
  }

  private def formatterCacheInputs(
      flags: Seq[String],
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String]): Seq[String] =
    Seq(javaHomeCacheInput) ++ javaArgs(flags, formatterClasspath, javaMaxHeap) ++ formatterClasspath.flatMap { file =>
      Seq(file.lastModified().toString, file.length().toString)
    }

  private def trackSourcesViaCache(
      cacheStoreFactory: CacheStoreFactory,
      sources: Seq[File],
      inputs: Seq[String],
      fixImportsOnly: Boolean)(f: (ChangeReport[File], Analysis) => Analysis): Analysis = {
    val cache = cacheStoreFactory.sub(if (fixImportsOnly) "imports-only" else "format")
    val last = cache.make("last")
    val outputDiff = cache.make("output-diff")
    val inputsTracker = Tracked.inputChanged[List[String], Analysis](cache.make("inputs")) { (changed, _) =>
      if (changed) {
        // Clear both source stamps and failed checks when the effective formatter invocation changes.
        last.delete()
        outputDiff.delete()
      }
      val prevTracker = Tracked.lastOutput[Unit, Analysis](last) { (_, prev0) =>
        val prev = prev0.getOrElse(Analysis(Set.empty))
        Tracked.diffOutputs(outputDiff, FileInfo.lastModified)(sources.toSet) { (outDiff: ChangeReport[File]) =>
          f(outDiff, prev)
        }
      }
      prevTracker(())
    }
    inputsTracker(inputs.toList)
  }

  private def cliFlags(
      options: JavaFormatterOptions,
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Seq[String] = {
    val styleFlags =
      if (options.style() == JavaFormatterOptions.Style.AOSP) Seq("--aosp")
      else Nil
    val sortImportsFlags =
      if (sortImports) Nil
      else Seq("--skip-sorting-imports")
    val removeUnusedImportsFlags =
      if (removeUnusedImports) Nil
      else Seq("--skip-removing-unused-imports")
    val importFlags = styleFlags ++ sortImportsFlags ++ removeUnusedImportsFlags
    if (fixImportsOnly) {
      importFlags ++ Seq("--fix-imports-only")
    } else {
      val javadocFlags =
        if (options.formatJavadoc()) Nil
        else Seq("--skip-javadoc-formatting")
      val reorderModifiersFlags =
        if (options.reorderModifiers()) Nil
        else Seq("--skip-reordering-modifiers")
      val reflowLongStringsFlags =
        if (reflowLongStrings) Nil
        else Seq("--skip-reflowing-long-strings")
      val maxLineLengthFlags =
        if (maxLineLength == DefaultMaxLineLength) Nil
        else Seq(s"--max-line-length=$maxLineLength")
      importFlags ++ javadocFlags ++ reorderModifiersFlags ++ reflowLongStringsFlags ++ maxLineLengthFlags
    }
  }

  private case class CliResult(exitCode: Int, stdout: Vector[String], stderr: Vector[String])

  private def logCliFailure(result: CliResult, log: Logger, projectId: String, invocationId: String): Unit = {
    result.stderr.foreach(line => log.error(line))
    result.stdout.foreach(line => log.error(line))
    incompatibleJavaRuntimeHelp(result).foreach { message =>
      val previouslyLoggedInvocation = incompatibleJavaRuntimeHelpLoggedByProject.putIfAbsent(projectId, invocationId)
      if (previouslyLoggedInvocation.forall(_ != invocationId)) {
        incompatibleJavaRuntimeHelpLoggedByProject.put(projectId, invocationId)
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
        s"or lower the sbt setting ThisBuild / javafmtFormatterCompatibleJavaVersion to match the available Java runtime. " +
        s"For configuration details and troubleshooting, see: https://github.com/sbt/sbt-java-formatter\n\n\n")
    } else if (olderFormatterOnNewerJdk) {
      Some(
        s"\n\n\nThe selected google-java-format runtime appears to be too old for the Java version used to launch the formatter JVM. " +
        s"Try increasing the sbt setting ThisBuild / javafmtFormatterCompatibleJavaVersion, " +
        s"or point the formatter to an older compatible JDK via the $JavaHomeEnvVar environment variable or -D$JavaHomeProperty=.... " +
        s"For configuration details and troubleshooting, see: https://github.com/sbt/sbt-java-formatter\n\n\n")
    } else {
      None
    }
  }

  private def javaHomeSourceAndPath: (String, String) =
    sys.props
      .get(JavaHomeProperty)
      .filter(_.nonEmpty)
      .map(path => (JavaHomeProperty, path))
      .orElse(sys.env.get(JavaHomeEnvVar).filter(_.nonEmpty).map(path => (JavaHomeEnvVar, path)))
      .getOrElse(("java.home", sys.props("java.home")))

  private def javaBin: String = {
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
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int,
      warnOnFailure: Boolean = true): Set[File] = {
    if (sources.isEmpty) {
      return Set.empty
    }
    val args =
      cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings, maxLineLength) ++ Seq(
        "--dry-run",
        "--set-exit-if-changed") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, formatterClasspath, log, javaMaxHeap)
    val changed = result.stdout.iterator.map(path => file(path)).toSet
    result.exitCode match {
      case 0 | 1 =>
        if (result.exitCode == 1 && changed.isEmpty) {
          logCliFailure(result, log, projectId, invocationId)
          throw new MessageOnlyException("google-java-format check failed")
        }
        changed
      case _ =>
        if (warnOnFailure) {
          logCliFailure(result, log, projectId, invocationId)
        }
        throw new MessageOnlyException("google-java-format check failed")
    }
  }

  private def runReplace(
      sources: Seq[File],
      log: Logger,
      projectId: String,
      invocationId: String,
      options: JavaFormatterOptions,
      formatterClasspath: Seq[File],
      javaMaxHeap: Option[String],
      fixImportsOnly: Boolean,
      sortImports: Boolean,
      removeUnusedImports: Boolean,
      reflowLongStrings: Boolean,
      maxLineLength: Int): Unit = {
    if (sources.isEmpty) {
      return
    }
    val args =
      cliFlags(options, fixImportsOnly, sortImports, removeUnusedImports, reflowLongStrings, maxLineLength) ++ Seq(
        "--replace") ++ sources.map(_.getAbsolutePath)
    val result = runCli(args, formatterClasspath, log, javaMaxHeap)
    if (result.exitCode != 0) {
      logCliFailure(result, log, projectId, invocationId)
      throw new MessageOnlyException("google-java-format failed")
    }
  }

}
