### Releasing

1. Check [closed issues without a milestone](https://github.com/sbt/sbt-java-formatter/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20no%3Amilestone) and either assign them the upcoming release milestone or 'invalid'
1. Create a [new release](https://github.com/sbt/sbt-java-formatter/releases/new) with:
    * the next tag version (e.g. `v0.4.0`)
    * title and release description including notable changes
    * link to the [milestone](https://github.com/sbt/sbt-java-formatter/milestones) showing an overview of closed issues for this release
    * overview of contributors generated by [`sbt-authors`](https://github.com/2m/authors)
1. Travis CI will start a [CI build](https://travis-ci.org/sbt/sbt-java-formatter/builds) for the new tag and publish artifacts to Bintray
1. Close the milestone for this release and create a new one