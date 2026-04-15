import com.google.googlejavaformat.java.JavaFormatterOptions

Compile / javafmtOptions := JavaFormatterOptions
  .builder()
  .style(javafmtStyle.value)
  .formatJavadoc(javafmtFormatJavadoc.value)
  .reorderModifiers(false)
  .build()
