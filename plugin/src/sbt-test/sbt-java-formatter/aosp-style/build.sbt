import com.palantir.javaformat.java.JavaFormatterOptions
// no settings needed

ThisBuild / javafmtStyle := JavaFormatterOptions.Style.AOSP
ThisBuild / javafmtOnCompile := true
