import com.palantir.javaformat.java.JavaFormatterOptions
// no settings needed

ThisBuild / javafmtStyle := JavaFormatterOptions.Style.PALANTIR
ThisBuild / javafmtOnCompile := true
