#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
build_file="$repo_root/build.sbt"
plugin_file="$repo_root/plugin/src/main/scala/com/github/sbt/JavaFormatterPlugin.scala"
readme_file="$repo_root/README.md"

compile_api_version="1.24.0"

# Assumption:
# the latest google-java-format release continues to belong to the Java 21
# compatibility bucket. If upstream raises the runtime requirement again,
# update this hook and the compatibility mapping policy.

extract_version() {
  sed -n 's/.*"com\.google\.googlejavaformat" % "google-java-format" % "\([^"]*\)".*/\1/p' "$build_file" | head -n1
}

detected_version="$(extract_version)"

if [[ -z "$detected_version" ]]; then
  echo "Could not determine google-java-format version from $build_file" >&2
  exit 1
fi

if [[ "$detected_version" == "$compile_api_version" ]]; then
  echo "google-java-format compile API version is unchanged at $compile_api_version; nothing to rewrite."
  exit 0
fi

perl -0pi -e 's/"com\.google\.googlejavaformat" % "google-java-format" % "[^"]*"/"com.google.googlejavaformat" % "google-java-format" % "'"$compile_api_version"'"/' "$build_file"
perl -0pi -e 's/case 21\s*=> "\Q'"$detected_version"'\E"|case 21\s*=> "[^"]*"/case 21    => "'"$detected_version"'"/' "$plugin_file"
perl -0pi -e 's/- `21` -> `google-java-format [^`]*`/- `21` -> `google-java-format '"$detected_version"'`/' "$readme_file"

echo "Updated runtime google-java-format version to $detected_version and restored build.sbt to $compile_api_version."
