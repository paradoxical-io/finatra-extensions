function snapshot() {
  sbt -Dversion="1.0.${TRAVIS_BUILD_NUMBER}-SNAPSHOT" publishSigned
}

function release() {
  sbt -Dversion=$REVISION publishSigned sonatypeReleaseAll
}
