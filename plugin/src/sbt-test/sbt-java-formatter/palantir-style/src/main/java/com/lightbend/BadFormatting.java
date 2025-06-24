package com.lightbend;

import java.util.stream.Collectors;
import java.util.Objects;
import java.util.List;

public     class BadFormatting {
  BadFormatting    () {example();}
  public    void example     () {
    var a = List.of("", "a", "b", "c", "d").stream().filter(s -> !s.isEmpty()).map(s -> String.format("%s-%s-%s", s, s.toLowerCase(), s.toUpperCase())).map(Objects::toString).collect(Collectors.joining());
  }
}
