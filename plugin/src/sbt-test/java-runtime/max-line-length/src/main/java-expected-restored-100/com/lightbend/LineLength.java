package com.lightbend;

/**
 * This documentation describes a method that checks two strings for their expected contents and
 * returns a useful example message for the caller.
 */
public class LineLength {
  private static final String MESSAGE =
      "This message contains enough words to exercise configurable line"
          + " wrapping in the formatter while remaining readable at different"
          + " column widths.";

  public String message() {
    return MESSAGE;
  }

  public boolean containsBoth(String firstValue, String secondValue) {
    return firstValue.contains("first value") && secondValue.contains("second value");
  }
}
