/*
 * Copyright 2015 Typesafe Inc.
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

import sbt._

object Version {
  val eclipseJdtCore = "3.10.0.v20140604-1726"
  val eclipseText    = "3.5.101"
}

object Library {
  val eclipseJdtCore = "org.eclipse.tycho" % "org.eclipse.jdt.core" % Version.eclipseJdtCore
  val eclipseText    = "org.eclipse.text"  % "org.eclipse.text"     % Version.eclipseText
}
