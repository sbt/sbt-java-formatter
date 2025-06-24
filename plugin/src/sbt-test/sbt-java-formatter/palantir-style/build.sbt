import com.github.sbt.javaformatter.Formatter
// no settings needed

ThisBuild / javafmtFormatter := Formatter.PALANTIR
ThisBuild / javafmtOnCompile := true
