/*
 * Copyright 2016 Lightbend Inc.
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

package com.typesafe.sbt

import com.typesafe.sbt.javaformatter.JavaFormatter
import com.typesafe.sbt.javaformatter.JavaFormatter.JavaFormatterSettings
import sbt._
import sbt.Keys._

object AutomateJavaFormatterPlugin extends AutoPlugin {
  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = automateFor(Compile, Test)

  def automateFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(compile := compile.dependsOn(JavaFormatterPlugin.JavaFormatterKeys.formatJava).value)
  }
}

object JavaFormatterPlugin extends AutoPlugin {

  object JavaFormatterKeys {
    val formatJava: TaskKey[Seq[File]] =
      TaskKey("java-formatter-format", "Format (Java) sources using the eclipse formatter")
    val settings: SettingKey[Map[String, String]] =
      SettingKey("java-formatter-settings", "A Map of eclipse formatter settings and values")
    val sourceLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-source-level", "Java source level. Overrides source level defined in settings.")
    val eclipseJdtCoreFormatterSettingPrefix: SettingKey[String] =
      SettingKey("eclipse-jdt-core-formatter-setting-prefix", "Prefix for all settings to be passed to Eclipse formatter")
    val targetLevel: SettingKey[Option[String]] =
      SettingKey("java-formatter-target-level", "Java target level. Overrides target level defined in settings.")
    val javaFormattingSettingsFile: SettingKey[Option[File]] =
      SettingKey("javaFormattingSettingsFile", "XML file with eclipse formatter settings.")

    object FormattingSettings extends javaformatter.FormattingSettings
  }

  val autoImport = JavaFormatterKeys
  import autoImport._

  override def trigger = allRequirements

  override def `requires` = plugins.JvmPlugin

  override def projectSettings = settingsFor(Compile, Test) ++ notToBeScopedSettings

  def settingsFor(configurations: Configuration*): Seq[Setting[_]] = configurations.foldLeft(List.empty[Setting[_]]) {
    _ ++ inConfig(_)(toBeScopedSettings)
  }

  private def setOrNone(value: String): Option[String] = if (value == "default") None else Some(value)

  def settingsFromProfile(file: File): Map[String, String] = {
    val xml = scala.xml.XML.loadFile(file)
    val map = (xml \\ "setting").foldLeft(Map.empty[String, String]) {
      case (r, node) => r.updated((node \ "@id").text, (node \ "@value").text)
    }

    map foreach { v =>
      println("   " + v)
    }

    map
  }

  def toBeScopedSettings: Seq[Setting[_]] =
    List(
      (sourceDirectories in formatJava) := List(javaSource.value),
      formatJava := {
        val formatterSettings = new JavaFormatterSettings(settings.value, sourceLevel.value, targetLevel.value)
        JavaFormatter(
          (sourceDirectories in formatJava).value.toList,
          (includeFilter in formatJava).value,
          (excludeFilter in formatJava).value,
          thisProjectRef.value,
          configuration.value,
          streams.value,
          formatterSettings)
      }
    )

  def notToBeScopedSettings: Seq[Setting[_]] =
    JavaFormatterKeys.FormattingSettings.formattingSettingsTyped ++
      List(
        includeFilter in formatJava := "*.java",
        sourceLevel := None,
        targetLevel := None,
        eclipseJdtCoreFormatterSettingPrefix := "org.eclipse.jdt.core.formatter.",
        javaFormattingSettingsFile := None,
        settings := {
          javaFormattingSettingsFile.value match {
            case Some(settingsXml) =>
              settingsFromProfile(settingsXml)
            case None =>
              import FormattingSettings._
              val prefix = eclipseJdtCoreFormatterSettingPrefix.value
              val map = Map(
                prefix + "alignment_for_arguments_in_allocation_expression" -> alignment_for_arguments_in_allocation_expression.value.toString,
                prefix + "alignment_for_arguments_in_annotation" -> alignment_for_arguments_in_annotation.value.toString,
                prefix + "alignment_for_arguments_in_enum_constant" -> alignment_for_arguments_in_enum_constant.value.toString,
                prefix + "alignment_for_arguments_in_explicit_constructor_call" -> alignment_for_arguments_in_explicit_constructor_call.value.toString,
                prefix + "alignment_for_arguments_in_method_invocation" -> alignment_for_arguments_in_method_invocation.value.toString,
                prefix + "alignment_for_arguments_in_qualified_allocation_expression" -> alignment_for_arguments_in_qualified_allocation_expression.value.toString,
                prefix + "alignment_for_assignment" -> alignment_for_assignment.value.toString,
                prefix + "alignment_for_binary_expression" -> alignment_for_binary_expression.value.toString,
                prefix + "alignment_for_compact_if" -> alignment_for_compact_if.value.toString,
                prefix + "alignment_for_conditional_expression" -> alignment_for_conditional_expression.value.toString,
                prefix + "alignment_for_enum_constants" -> alignment_for_enum_constants.value.toString,
                prefix + "alignment_for_expressions_in_array_initializer" -> alignment_for_expressions_in_array_initializer.value.toString,
                prefix + "alignment_for_method_declaration" -> alignment_for_method_declaration.value.toString,
                prefix + "alignment_for_multiple_fields" -> alignment_for_multiple_fields.value.toString,
                prefix + "alignment_for_parameters_in_constructor_declaration" -> alignment_for_parameters_in_constructor_declaration.value.toString,
                prefix + "alignment_for_parameters_in_method_declaration" -> alignment_for_parameters_in_method_declaration.value.toString,
                prefix + "alignment_for_selector_in_method_invocation" -> alignment_for_selector_in_method_invocation.value.toString,
                prefix + "alignment_for_superclass_in_type_declaration" -> alignment_for_superclass_in_type_declaration.value.toString,
                prefix + "alignment_for_superinterfaces_in_enum_declaration" -> alignment_for_superinterfaces_in_enum_declaration.value.toString,
                prefix + "alignment_for_superinterfaces_in_type_declaration" -> alignment_for_superinterfaces_in_type_declaration.value.toString,
                prefix + "alignment_for_throws_clause_in_constructor_declaration" -> alignment_for_throws_clause_in_constructor_declaration.value.toString,
                prefix + "alignment_for_throws_clause_in_method_declaration" -> alignment_for_throws_clause_in_method_declaration.value.toString,
                prefix + "alignment_for_resources_in_try" -> alignment_for_resources_in_try.value.toString,
                prefix + "alignment_for_union_type_in_multicatch" -> alignment_for_union_type_in_multicatch.value.toString,
                prefix + "align_type_members_on_columns" -> align_type_members_on_columns.value.toString,
                prefix + "brace_position_for_annotation_type_declaration" -> brace_position_for_annotation_type_declaration.value.toString,
                prefix + "brace_position_for_anonymous_type_declaration" -> brace_position_for_anonymous_type_declaration.value.toString,
                prefix + "brace_position_for_array_initializer" -> brace_position_for_array_initializer.value.toString,
                prefix + "brace_position_for_block" -> brace_position_for_block.value.toString,
                prefix + "brace_position_for_block_in_case" -> brace_position_for_block_in_case.value.toString,
                prefix + "brace_position_for_constructor_declaration" -> brace_position_for_constructor_declaration.value.toString,
                prefix + "brace_position_for_enum_constant" -> brace_position_for_enum_constant.value.toString,
                prefix + "brace_position_for_enum_declaration" -> brace_position_for_enum_declaration.value.toString,
                prefix + "brace_position_for_lambda_body" -> brace_position_for_lambda_body.value.toString,
                prefix + "brace_position_for_method_declaration" -> brace_position_for_method_declaration.value.toString,
                prefix + "brace_position_for_type_declaration" -> brace_position_for_type_declaration.value.toString,
                prefix + "brace_position_for_switch" -> brace_position_for_switch.value.toString,
                prefix + "continuation_indentation" -> continuation_indentation.value.toString,
                prefix + "continuation_indentation_for_array_initializer" -> continuation_indentation_for_array_initializer.value.toString,
                prefix + "blank_lines_after_imports" -> blank_lines_after_imports.value.toString,
                prefix + "blank_lines_after_package" -> blank_lines_after_package.value.toString,
                prefix + "blank_lines_before_field" -> blank_lines_before_field.value.toString,
                prefix + "blank_lines_before_first_class_body_declaration" -> blank_lines_before_first_class_body_declaration.value.toString,
                prefix + "blank_lines_before_imports" -> blank_lines_before_imports.value.toString,
                prefix + "blank_lines_before_member_type" -> blank_lines_before_member_type.value.toString,
                prefix + "blank_lines_before_method" -> blank_lines_before_method.value.toString,
                prefix + "blank_lines_before_new_chunk" -> blank_lines_before_new_chunk.value.toString,
                prefix + "blank_lines_before_package" -> blank_lines_before_package.value.toString,
                prefix + "blank_lines_between_import_groups" -> blank_lines_between_import_groups.value.toString,
                prefix + "blank_lines_between_type_declarations" -> blank_lines_between_type_declarations.value.toString,
                prefix + "blank_lines_at_beginning_of_method_body" -> blank_lines_at_beginning_of_method_body.value.toString,
                prefix + "comment.clear_blank_lines_in_javadoc_comment" -> comment_clear_blank_lines_in_javadoc_comment.value.toString,
                prefix + "comment.clear_blank_lines_in_block_comment" -> comment_clear_blank_lines_in_block_comment.value.toString,
                prefix + "comment.new_lines_at_block_boundaries" -> comment_new_lines_at_block_boundaries.value.toString,
                prefix + "comment.new_lines_at_javadoc_boundaries" -> comment_new_lines_at_javadoc_boundaries.value.toString,
                prefix + "comment.format_javadoc_comment" -> comment_format_javadoc_comment.value.toString,
                prefix + "comment.format_line_comment" -> comment_format_line_comment.value.toString,
                prefix + "comment.format_line_comment_starting_on_first_column" -> comment_format_line_comment_starting_on_first_column.value.toString,
                prefix + "comment.format_block_comment" -> comment_format_block_comment.value.toString,
                prefix + "comment.format_header" -> comment_format_header.value.toString,
                prefix + "comment.format_html" -> comment_format_html.value.toString,
                prefix + "comment.format_source" -> comment_format_source.value.toString,
                prefix + "comment.indent_parameter_description" -> comment_indent_parameter_description.value.toString,
                prefix + "comment.indent_root_tags" -> comment_indent_root_tags.value.toString,
                prefix + "comment.insert_empty_line_before_root_tags" -> comment_insert_empty_line_before_root_tags.value.toString,
                prefix + "comment.insert_new_line_for_parameter" -> comment_insert_new_line_for_parameter.value.toString,
                prefix + "comment.preserve_white_space_between_code_and_line_comments" -> comment_preserve_white_space_between_code_and_line_comments.value.toString,
                prefix + "comment.line_length" -> comment_line_length.value.toString,
                prefix + "line_split" -> line_split.value.toString,
                prefix + "tabulation.size" -> tabulation_size.value.toString,
                prefix + "use_tags" -> use_tags.value.toString,
                prefix + "disabling_tag" -> disabling_tag.value.toString,
                prefix + "enabling_tag" -> enabling_tag.value.toString,
                prefix + "indent_statements_compare_to_block" -> indent_statements_compare_to_block.value.toString,
                prefix + "indent_statements_compare_to_body" -> indent_statements_compare_to_body.value.toString,
                prefix + "indent_body_declarations_compare_to_annotation_declaration_header" -> indent_body_declarations_compare_to_annotation_declaration_header.value.toString,
                prefix + "indent_body_declarations_compare_to_enum_constant_header" -> indent_body_declarations_compare_to_enum_constant_header.value.toString,
                prefix + "indent_body_declarations_compare_to_enum_declaration_header" -> indent_body_declarations_compare_to_enum_declaration_header.value.toString,
                prefix + "indent_body_declarations_compare_to_type_header" -> indent_body_declarations_compare_to_type_header.value.toString,
                prefix + "indent_breaks_compare_to_cases" -> indent_breaks_compare_to_cases.value.toString,
                prefix + "indent_empty_lines" -> indent_empty_lines.value.toString,
                prefix + "indent_switchstatements_compare_to_cases" -> indent_switchstatements_compare_to_cases.value.toString,
                prefix + "indent_switchstatements_compare_to_switch" -> indent_switchstatements_compare_to_switch.value.toString,
                prefix + "indentation.size" -> indentation_size.value.toString,
                prefix + "indentation.char" -> indentation_char.value.toString,
                prefix + "insert_new_line_after_annotation_on_type" -> insert_new_line_after_annotation_on_type.value.toString,
                prefix + "insert_new_line_after_type_annotation" -> insert_new_line_after_type_annotation.value.toString,
                prefix + "insert_new_line_after_annotation_on_field" -> insert_new_line_after_annotation_on_field.value.toString,
                prefix + "insert_new_line_after_annotation_on_method" -> insert_new_line_after_annotation_on_method.value.toString,
                prefix + "insert_new_line_after_annotation_on_package" -> insert_new_line_after_annotation_on_package.value.toString,
                prefix + "insert_new_line_after_annotation_on_parameter" -> insert_new_line_after_annotation_on_parameter.value.toString,
                prefix + "insert_new_line_after_annotation_on_local_variable" -> insert_new_line_after_annotation_on_local_variable.value.toString,
                prefix + "insert_new_line_after_label" -> insert_new_line_after_label.value.toString,
                prefix + "insert_new_line_after_opening_brace_in_array_initializer" -> insert_new_line_after_opening_brace_in_array_initializer.value.toString,
                prefix + "insert_new_line_at_end_of_file_if_missing" -> insert_new_line_at_end_of_file_if_missing.value.toString,
                prefix + "insert_new_line_before_catch_in_try_statement" -> insert_new_line_before_catch_in_try_statement.value.toString,
                prefix + "insert_new_line_before_closing_brace_in_array_initializer" -> insert_new_line_before_closing_brace_in_array_initializer.value.toString,
                prefix + "insert_new_line_before_else_in_if_statement" -> insert_new_line_before_else_in_if_statement.value.toString,
                prefix + "insert_new_line_before_finally_in_try_statement" -> insert_new_line_before_finally_in_try_statement.value.toString,
                prefix + "insert_new_line_before_while_in_do_statement" -> insert_new_line_before_while_in_do_statement.value.toString,
                prefix + "insert_new_line_in_empty_anonymous_type_declaration" -> insert_new_line_in_empty_anonymous_type_declaration.value.toString,
                prefix + "insert_new_line_in_empty_block" -> insert_new_line_in_empty_block.value.toString,
                prefix + "insert_new_line_in_empty_annotation_declaration" -> insert_new_line_in_empty_annotation_declaration.value.toString,
                prefix + "insert_new_line_in_empty_enum_constant" -> insert_new_line_in_empty_enum_constant.value.toString,
                prefix + "insert_new_line_in_empty_enum_declaration" -> insert_new_line_in_empty_enum_declaration.value.toString,
                prefix + "insert_new_line_in_empty_method_body" -> insert_new_line_in_empty_method_body.value.toString,
                prefix + "insert_new_line_in_empty_type_declaration" -> insert_new_line_in_empty_type_declaration.value.toString,
                prefix + "insert_space_after_and_in_type_parameter" -> insert_space_after_and_in_type_parameter.value.toString,
                prefix + "insert_space_after_assignment_operator" -> insert_space_after_assignment_operator.value.toString,
                prefix + "insert_space_after_at_in_annotation" -> insert_space_after_at_in_annotation.value.toString,
                prefix + "insert_space_after_at_in_annotation_type_declaration" -> insert_space_after_at_in_annotation_type_declaration.value.toString,
                prefix + "insert_space_after_binary_operator" -> insert_space_after_binary_operator.value.toString,
                prefix + "insert_space_after_closing_angle_bracket_in_type_arguments" -> insert_space_after_closing_angle_bracket_in_type_arguments.value.toString,
                prefix + "insert_space_after_closing_angle_bracket_in_type_parameters" -> insert_space_after_closing_angle_bracket_in_type_parameters.value.toString,
                prefix + "insert_space_after_closing_paren_in_cast" -> insert_space_after_closing_paren_in_cast.value.toString,
                prefix + "insert_space_after_closing_brace_in_block" -> insert_space_after_closing_brace_in_block.value.toString,
                prefix + "insert_space_after_colon_in_assert" -> insert_space_after_colon_in_assert.value.toString,
                prefix + "insert_space_after_colon_in_case" -> insert_space_after_colon_in_case.value.toString,
                prefix + "insert_space_after_colon_in_conditional" -> insert_space_after_colon_in_conditional.value.toString,
                prefix + "insert_space_after_colon_in_for" -> insert_space_after_colon_in_for.value.toString,
                prefix + "insert_space_after_colon_in_labeled_statement" -> insert_space_after_colon_in_labeled_statement.value.toString,
                prefix + "insert_space_after_comma_in_allocation_expression" -> insert_space_after_comma_in_allocation_expression.value.toString,
                prefix + "insert_space_after_comma_in_annotation" -> insert_space_after_comma_in_annotation.value.toString,
                prefix + "insert_space_after_comma_in_array_initializer" -> insert_space_after_comma_in_array_initializer.value.toString,
                prefix + "insert_space_after_comma_in_constructor_declaration_parameters" -> insert_space_after_comma_in_constructor_declaration_parameters.value.toString,
                prefix + "insert_space_after_comma_in_constructor_declaration_throws" -> insert_space_after_comma_in_constructor_declaration_throws.value.toString,
                prefix + "insert_space_after_comma_in_enum_constant_arguments" -> insert_space_after_comma_in_enum_constant_arguments.value.toString,
                prefix + "insert_space_after_comma_in_enum_declarations" -> insert_space_after_comma_in_enum_declarations.value.toString,
                prefix + "insert_space_after_comma_in_explicit_constructor_call_arguments" -> insert_space_after_comma_in_explicit_constructor_call_arguments.value.toString,
                prefix + "insert_space_after_comma_in_for_increments" -> insert_space_after_comma_in_for_increments.value.toString,
                prefix + "insert_space_after_comma_in_for_inits" -> insert_space_after_comma_in_for_inits.value.toString,
                prefix + "insert_space_after_comma_in_method_invocation_arguments" -> insert_space_after_comma_in_method_invocation_arguments.value.toString,
                prefix + "insert_space_after_comma_in_method_declaration_parameters" -> insert_space_after_comma_in_method_declaration_parameters.value.toString,
                prefix + "insert_space_after_comma_in_method_declaration_throws" -> insert_space_after_comma_in_method_declaration_throws.value.toString,
                prefix + "insert_space_after_comma_in_multiple_field_declarations" -> insert_space_after_comma_in_multiple_field_declarations.value.toString,
                prefix + "insert_space_after_comma_in_multiple_local_declarations" -> insert_space_after_comma_in_multiple_local_declarations.value.toString,
                prefix + "insert_space_after_comma_in_parameterized_type_reference" -> insert_space_after_comma_in_parameterized_type_reference.value.toString,
                prefix + "insert_space_after_comma_in_superinterfaces" -> insert_space_after_comma_in_superinterfaces.value.toString,
                prefix + "insert_space_after_comma_in_type_arguments" -> insert_space_after_comma_in_type_arguments.value.toString,
                prefix + "insert_space_after_comma_in_type_parameters" -> insert_space_after_comma_in_type_parameters.value.toString,
                prefix + "insert_space_after_ellipsis" -> insert_space_after_ellipsis.value.toString,
                prefix + "insert_space_after_lambda_arrow" -> insert_space_after_lambda_arrow.value.toString,
                prefix + "insert_space_after_opening_angle_bracket_in_parameterized_type_reference" -> insert_space_after_opening_angle_bracket_in_parameterized_type_reference.value.toString,
                prefix + "insert_space_after_opening_angle_bracket_in_type_arguments" -> insert_space_after_opening_angle_bracket_in_type_arguments.value.toString,
                prefix + "insert_space_after_opening_angle_bracket_in_type_parameters" -> insert_space_after_opening_angle_bracket_in_type_parameters.value.toString,
                prefix + "insert_space_after_opening_bracket_in_array_allocation_expression" -> insert_space_after_opening_bracket_in_array_allocation_expression.value.toString,
                prefix + "insert_space_after_opening_bracket_in_array_reference" -> insert_space_after_opening_bracket_in_array_reference.value.toString,
                prefix + "insert_space_after_opening_brace_in_array_initializer" -> insert_space_after_opening_brace_in_array_initializer.value.toString,
                prefix + "insert_space_after_opening_paren_in_annotation" -> insert_space_after_opening_paren_in_annotation.value.toString,
                prefix + "insert_space_after_opening_paren_in_cast" -> insert_space_after_opening_paren_in_cast.value.toString,
                prefix + "insert_space_after_opening_paren_in_catch" -> insert_space_after_opening_paren_in_catch.value.toString,
                prefix + "insert_space_after_opening_paren_in_constructor_declaration" -> insert_space_after_opening_paren_in_constructor_declaration.value.toString,
                prefix + "insert_space_after_opening_paren_in_enum_constant" -> insert_space_after_opening_paren_in_enum_constant.value.toString,
                prefix + "insert_space_after_opening_paren_in_for" -> insert_space_after_opening_paren_in_for.value.toString,
                prefix + "insert_space_after_opening_paren_in_if" -> insert_space_after_opening_paren_in_if.value.toString,
                prefix + "insert_space_after_opening_paren_in_method_declaration" -> insert_space_after_opening_paren_in_method_declaration.value.toString,
                prefix + "insert_space_after_opening_paren_in_method_invocation" -> insert_space_after_opening_paren_in_method_invocation.value.toString,
                prefix + "insert_space_after_opening_paren_in_parenthesized_expression" -> insert_space_after_opening_paren_in_parenthesized_expression.value.toString,
                prefix + "insert_space_after_opening_paren_in_switch" -> insert_space_after_opening_paren_in_switch.value.toString,
                prefix + "insert_space_after_opening_paren_in_synchronized" -> insert_space_after_opening_paren_in_synchronized.value.toString,
                prefix + "insert_space_after_opening_paren_in_try" -> insert_space_after_opening_paren_in_try.value.toString,
                prefix + "insert_space_after_opening_paren_in_while" -> insert_space_after_opening_paren_in_while.value.toString,
                prefix + "insert_space_after_postfix_operator" -> insert_space_after_postfix_operator.value.toString,
                prefix + "insert_space_after_prefix_operator" -> insert_space_after_prefix_operator.value.toString,
                prefix + "insert_space_after_question_in_conditional" -> insert_space_after_question_in_conditional.value.toString,
                prefix + "insert_space_after_question_in_wilcard" -> insert_space_after_question_in_wilcard.value.toString,
                prefix + "insert_space_after_semicolon_in_for" -> insert_space_after_semicolon_in_for.value.toString,
                prefix + "insert_space_after_semicolon_in_try_resources" -> insert_space_after_semicolon_in_try_resources.value.toString,
                prefix + "insert_space_after_unary_operator" -> insert_space_after_unary_operator.value.toString,
                prefix + "insert_space_before_and_in_type_parameter" -> insert_space_before_and_in_type_parameter.value.toString,
                prefix + "insert_space_before_at_in_annotation_type_declaration" -> insert_space_before_at_in_annotation_type_declaration.value.toString,
                prefix + "insert_space_before_assignment_operator" -> insert_space_before_assignment_operator.value.toString,
                prefix + "insert_space_before_binary_operator" -> insert_space_before_binary_operator.value.toString,
                prefix + "insert_space_before_closing_angle_bracket_in_parameterized_type_reference" -> insert_space_before_closing_angle_bracket_in_parameterized_type_reference.value.toString,
                prefix + "insert_space_before_closing_angle_bracket_in_type_arguments" -> insert_space_before_closing_angle_bracket_in_type_arguments.value.toString,
                prefix + "insert_space_before_closing_angle_bracket_in_type_parameters" -> insert_space_before_closing_angle_bracket_in_type_parameters.value.toString,
                prefix + "insert_space_before_closing_brace_in_array_initializer" -> insert_space_before_closing_brace_in_array_initializer.value.toString,
                prefix + "insert_space_before_closing_bracket_in_array_allocation_expression" -> insert_space_before_closing_bracket_in_array_allocation_expression.value.toString,
                prefix + "insert_space_before_closing_bracket_in_array_reference" -> insert_space_before_closing_bracket_in_array_reference.value.toString,
                prefix + "insert_space_before_closing_paren_in_annotation" -> insert_space_before_closing_paren_in_annotation.value.toString,
                prefix + "insert_space_before_closing_paren_in_cast" -> insert_space_before_closing_paren_in_cast.value.toString,
                prefix + "insert_space_before_closing_paren_in_catch" -> insert_space_before_closing_paren_in_catch.value.toString,
                prefix + "insert_space_before_closing_paren_in_constructor_declaration" -> insert_space_before_closing_paren_in_constructor_declaration.value.toString,
                prefix + "insert_space_before_closing_paren_in_enum_constant" -> insert_space_before_closing_paren_in_enum_constant.value.toString,
                prefix + "insert_space_before_closing_paren_in_for" -> insert_space_before_closing_paren_in_for.value.toString,
                prefix + "insert_space_before_closing_paren_in_if" -> insert_space_before_closing_paren_in_if.value.toString,
                prefix + "insert_space_before_closing_paren_in_method_declaration" -> insert_space_before_closing_paren_in_method_declaration.value.toString,
                prefix + "insert_space_before_closing_paren_in_method_invocation" -> insert_space_before_closing_paren_in_method_invocation.value.toString,
                prefix + "insert_space_before_closing_paren_in_parenthesized_expression" -> insert_space_before_closing_paren_in_parenthesized_expression.value.toString,
                prefix + "insert_space_before_closing_paren_in_switch" -> insert_space_before_closing_paren_in_switch.value.toString,
                prefix + "insert_space_before_closing_paren_in_synchronized" -> insert_space_before_closing_paren_in_synchronized.value.toString,
                prefix + "insert_space_before_closing_paren_in_try" -> insert_space_before_closing_paren_in_try.value.toString,
                prefix + "insert_space_before_closing_paren_in_while" -> insert_space_before_closing_paren_in_while.value.toString,
                prefix + "insert_space_before_colon_in_assert" -> insert_space_before_colon_in_assert.value.toString,
                prefix + "insert_space_before_colon_in_case" -> insert_space_before_colon_in_case.value.toString,
                prefix + "insert_space_before_colon_in_conditional" -> insert_space_before_colon_in_conditional.value.toString,
                prefix + "insert_space_before_colon_in_default" -> insert_space_before_colon_in_default.value.toString,
                prefix + "insert_space_before_colon_in_for" -> insert_space_before_colon_in_for.value.toString,
                prefix + "insert_space_before_colon_in_labeled_statement" -> insert_space_before_colon_in_labeled_statement.value.toString,
                prefix + "insert_space_before_comma_in_allocation_expression" -> insert_space_before_comma_in_allocation_expression.value.toString,
                prefix + "insert_space_before_comma_in_annotation" -> insert_space_before_comma_in_annotation.value.toString,
                prefix + "insert_space_before_comma_in_array_initializer" -> insert_space_before_comma_in_array_initializer.value.toString,
                prefix + "insert_space_before_comma_in_constructor_declaration_parameters" -> insert_space_before_comma_in_constructor_declaration_parameters.value.toString,
                prefix + "insert_space_before_comma_in_constructor_declaration_throws" -> insert_space_before_comma_in_constructor_declaration_throws.value.toString,
                prefix + "insert_space_before_comma_in_enum_constant_arguments" -> insert_space_before_comma_in_enum_constant_arguments.value.toString,
                prefix + "insert_space_before_comma_in_enum_declarations" -> insert_space_before_comma_in_enum_declarations.value.toString,
                prefix + "insert_space_before_comma_in_explicit_constructor_call_arguments" -> insert_space_before_comma_in_explicit_constructor_call_arguments.value.toString,
                prefix + "insert_space_before_comma_in_for_increments" -> insert_space_before_comma_in_for_increments.value.toString,
                prefix + "insert_space_before_comma_in_for_inits" -> insert_space_before_comma_in_for_inits.value.toString,
                prefix + "insert_space_before_comma_in_method_invocation_arguments" -> insert_space_before_comma_in_method_invocation_arguments.value.toString,
                prefix + "insert_space_before_comma_in_method_declaration_parameters" -> insert_space_before_comma_in_method_declaration_parameters.value.toString,
                prefix + "insert_space_before_comma_in_method_declaration_throws" -> insert_space_before_comma_in_method_declaration_throws.value.toString,
                prefix + "insert_space_before_comma_in_multiple_field_declarations" -> insert_space_before_comma_in_multiple_field_declarations.value.toString,
                prefix + "insert_space_before_comma_in_multiple_local_declarations" -> insert_space_before_comma_in_multiple_local_declarations.value.toString,
                prefix + "insert_space_before_comma_in_parameterized_type_reference" -> insert_space_before_comma_in_parameterized_type_reference.value.toString,
                prefix + "insert_space_before_comma_in_superinterfaces" -> insert_space_before_comma_in_superinterfaces.value.toString,
                prefix + "insert_space_before_comma_in_type_arguments" -> insert_space_before_comma_in_type_arguments.value.toString,
                prefix + "insert_space_before_comma_in_type_parameters" -> insert_space_before_comma_in_type_parameters.value.toString,
                prefix + "insert_space_before_ellipsis" -> insert_space_before_ellipsis.value.toString,
                prefix + "insert_space_before_lambda_arrow" -> insert_space_before_lambda_arrow.value.toString,
                prefix + "insert_space_before_parenthesized_expression_in_return" -> insert_space_before_parenthesized_expression_in_return.value.toString,
                prefix + "insert_space_before_parenthesized_expression_in_throw" -> insert_space_before_parenthesized_expression_in_throw.value.toString,
                prefix + "insert_space_before_question_in_wilcard" -> insert_space_before_question_in_wilcard.value.toString,
                prefix + "insert_space_before_opening_angle_bracket_in_parameterized_type_reference" -> insert_space_before_opening_angle_bracket_in_parameterized_type_reference.value.toString,
                prefix + "insert_space_before_opening_angle_bracket_in_type_arguments" -> insert_space_before_opening_angle_bracket_in_type_arguments.value.toString,
                prefix + "insert_space_before_opening_angle_bracket_in_type_parameters" -> insert_space_before_opening_angle_bracket_in_type_parameters.value.toString,
                prefix + "insert_space_before_opening_brace_in_annotation_type_declaration" -> insert_space_before_opening_brace_in_annotation_type_declaration.value.toString,
                prefix + "insert_space_before_opening_brace_in_anonymous_type_declaration" -> insert_space_before_opening_brace_in_anonymous_type_declaration.value.toString,
                prefix + "insert_space_before_opening_brace_in_array_initializer" -> insert_space_before_opening_brace_in_array_initializer.value.toString,
                prefix + "insert_space_before_opening_brace_in_block" -> insert_space_before_opening_brace_in_block.value.toString,
                prefix + "insert_space_before_opening_brace_in_constructor_declaration" -> insert_space_before_opening_brace_in_constructor_declaration.value.toString,
                prefix + "insert_space_before_opening_brace_in_enum_constant" -> insert_space_before_opening_brace_in_enum_constant.value.toString,
                prefix + "insert_space_before_opening_brace_in_enum_declaration" -> insert_space_before_opening_brace_in_enum_declaration.value.toString,
                prefix + "insert_space_before_opening_brace_in_method_declaration" -> insert_space_before_opening_brace_in_method_declaration.value.toString,
                prefix + "insert_space_before_opening_brace_in_type_declaration" -> insert_space_before_opening_brace_in_type_declaration.value.toString,
                prefix + "insert_space_before_opening_bracket_in_array_allocation_expression" -> insert_space_before_opening_bracket_in_array_allocation_expression.value.toString,
                prefix + "insert_space_before_opening_bracket_in_array_reference" -> insert_space_before_opening_bracket_in_array_reference.value.toString,
                prefix + "insert_space_before_opening_bracket_in_array_type_reference" -> insert_space_before_opening_bracket_in_array_type_reference.value.toString,
                prefix + "insert_space_before_opening_paren_in_annotation" -> insert_space_before_opening_paren_in_annotation.value.toString,
                prefix + "insert_space_before_opening_paren_in_annotation_type_member_declaration" -> insert_space_before_opening_paren_in_annotation_type_member_declaration.value.toString,
                prefix + "insert_space_before_opening_paren_in_catch" -> insert_space_before_opening_paren_in_catch.value.toString,
                prefix + "insert_space_before_opening_paren_in_constructor_declaration" -> insert_space_before_opening_paren_in_constructor_declaration.value.toString,
                prefix + "insert_space_before_opening_paren_in_enum_constant" -> insert_space_before_opening_paren_in_enum_constant.value.toString,
                prefix + "insert_space_before_opening_paren_in_for" -> insert_space_before_opening_paren_in_for.value.toString,
                prefix + "insert_space_before_opening_paren_in_if" -> insert_space_before_opening_paren_in_if.value.toString,
                prefix + "insert_space_before_opening_paren_in_method_invocation" -> insert_space_before_opening_paren_in_method_invocation.value.toString,
                prefix + "insert_space_before_opening_paren_in_method_declaration" -> insert_space_before_opening_paren_in_method_declaration.value.toString,
                prefix + "insert_space_before_opening_paren_in_switch" -> insert_space_before_opening_paren_in_switch.value.toString,
                prefix + "insert_space_before_opening_paren_in_try" -> insert_space_before_opening_paren_in_try.value.toString,
                prefix + "insert_space_before_opening_brace_in_switch" -> insert_space_before_opening_brace_in_switch.value.toString,
                prefix + "insert_space_before_opening_paren_in_synchronized" -> insert_space_before_opening_paren_in_synchronized.value.toString,
                prefix + "insert_space_before_opening_paren_in_parenthesized_expression" -> insert_space_before_opening_paren_in_parenthesized_expression.value.toString,
                prefix + "insert_space_before_opening_paren_in_while" -> insert_space_before_opening_paren_in_while.value.toString,
                prefix + "insert_space_before_postfix_operator" -> insert_space_before_postfix_operator.value.toString,
                prefix + "insert_space_before_prefix_operator" -> insert_space_before_prefix_operator.value.toString,
                prefix + "insert_space_before_question_in_conditional" -> insert_space_before_question_in_conditional.value.toString,
                prefix + "insert_space_before_semicolon" -> insert_space_before_semicolon.value.toString,
                prefix + "insert_space_before_semicolon_in_for" -> insert_space_before_semicolon_in_for.value.toString,
                prefix + "insert_space_before_semicolon_in_try_resources" -> insert_space_before_semicolon_in_try_resources.value.toString,
                prefix + "insert_space_before_unary_operator" -> insert_space_before_unary_operator.value.toString,
                prefix + "insert_space_between_brackets_in_array_type_reference" -> insert_space_between_brackets_in_array_type_reference.value.toString,
                prefix + "insert_space_between_empty_braces_in_array_initializer" -> insert_space_between_empty_braces_in_array_initializer.value.toString,
                prefix + "insert_space_between_empty_brackets_in_array_allocation_expression" -> insert_space_between_empty_brackets_in_array_allocation_expression.value.toString,
                prefix + "insert_space_between_empty_parens_in_annotation_type_member_declaration" -> insert_space_between_empty_parens_in_annotation_type_member_declaration.value.toString,
                prefix + "insert_space_between_empty_parens_in_constructor_declaration" -> insert_space_between_empty_parens_in_constructor_declaration.value.toString,
                prefix + "insert_space_between_empty_parens_in_enum_constant" -> insert_space_between_empty_parens_in_enum_constant.value.toString,
                prefix + "insert_space_between_empty_parens_in_method_declaration" -> insert_space_between_empty_parens_in_method_declaration.value.toString,
                prefix + "insert_space_between_empty_parens_in_method_invocation" -> insert_space_between_empty_parens_in_method_invocation.value.toString,
                prefix + "compact_else_if" -> compact_else_if.value.toString,
                prefix + "keep_guardian_clause_on_one_line" -> keep_guardian_clause_on_one_line.value.toString,
                prefix + "keep_else_statement_on_same_line" -> keep_else_statement_on_same_line.value.toString,
                prefix + "keep_empty_array_initializer_on_one_line" -> keep_empty_array_initializer_on_one_line.value.toString,
                prefix + "keep_simple_if_on_one_line" -> keep_simple_if_on_one_line.value.toString,
                prefix + "keep_then_statement_on_same_line" -> keep_then_statement_on_same_line.value.toString,
                prefix + "never_indent_block_comments_on_first_column" -> never_indent_block_comments_on_first_column.value.toString,
                prefix + "never_indent_line_comments_on_first_column" -> never_indent_line_comments_on_first_column.value.toString,
                prefix + "number_of_empty_lines_to_preserve" -> number_of_empty_lines_to_preserve.value.toString,
                prefix + "join_wrapped_lines" -> join_wrapped_lines.value.toString,
                prefix + "join_lines_in_comments" -> join_lines_in_comments.value.toString,
                prefix + "put_empty_statement_on_new_line" -> put_empty_statement_on_new_line.value.toString,
                prefix + "tabulation.size" -> tab_size.value.toString,
                prefix + "filling_space" -> filling_space.value.toString,
                prefix + "page_width" -> page_width.value.toString,
                prefix + "tabulation.char" -> tab_char.value.toString,
                prefix + "use_tabs_only_for_leading_indentations" -> use_tabs_only_for_leading_indentations.value.toString,
                prefix + "wrap_before_binary_operator" -> wrap_before_binary_operator.value.toString,
                prefix + "wrap_before_or_operator_multicatch" -> wrap_before_or_operator_multicatch.value.toString,
                prefix + "wrap_outer_expressions_when_nested" -> wrap_outer_expressions_when_nested.value.toString,
                prefix + "initial_indentation_level" -> initial_indentation_level.value.toString,
                prefix + "line_separator" -> line_separator.value.toString
              )

              map foreach { v =>
                println("   " + v)
              }

              map
          }
        }
      )
}
