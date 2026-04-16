import com.google.googlejavaformat.java.JavaFormatterOptions

ThisBuild / javafmtFormatterCompatibleJavaVersion := 11

Compile / javafmtOptions := JavaFormatterOptions
  .builder()
  .style(javafmtStyle.value)
  .formatJavadoc(javafmtFormatJavadoc.value)
  .reorderModifiers(false)
  .build()
