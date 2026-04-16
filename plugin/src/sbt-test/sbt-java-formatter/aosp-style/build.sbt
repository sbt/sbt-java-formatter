import com.google.googlejavaformat.java.JavaFormatterOptions
ThisBuild / javafmtFormatterCompatibleJavaVersion := 11

ThisBuild / javafmtStyle := JavaFormatterOptions.Style.AOSP
ThisBuild / javafmtOnCompile := true
