import com.google.googlejavaformat.java.JavaFormatterOptions
// no settings needed

ThisBuild / javafmtStyle := JavaFormatterOptions.Style.AOSP
ThisBuild / javafmtOnCompile := true
