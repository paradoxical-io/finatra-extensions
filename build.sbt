import BuildConfig.{Dependencies, versions}
import sbt._

lazy val commonSettings = BuildConfig.commonSettings()

lazy val `finatra-test` = project.in(file("finatra-test")).
  settings(commonSettings).
  settings(
    name := "finatra-test",
    libraryDependencies ++= Seq(
      "com.twitter" %% "inject-app" % versions.finatra,
      "com.twitter" %% "inject-core" % versions.finatra,
      "com.twitter" %% "finatra-http" % versions.finatra,
      "com.twitter" %% "inject-modules" % versions.finatra,
      "com.twitter" %% "inject-server" % versions.finatra,
      "com.twitter" %% "inject-app" % versions.finatra classifier "tests",
      "com.twitter" %% "inject-core" % versions.finatra classifier "tests",
      "com.twitter" %% "finatra-http" % versions.finatra classifier "tests",
      "com.twitter" %% "finatra-thrift" % versions.finatra classifier "tests",
      "com.twitter" %% "inject-modules" % versions.finatra classifier "tests",
      "com.twitter" %% "inject-server" % versions.finatra classifier "tests"
    )
  )

lazy val `finatra-server` = project.in(file("finatra-server")).
  settings(commonSettings).
  settings(
    name := "finatra-server",
    libraryDependencies ++= {
      Dependencies.requiredFinatraDeps ++
      Seq(
        "com.google.guava" % "guava" % versions.guava,
        "io.paradoxical" %% "paradox-scala-global" % versions.paradoxGlobal,
        "io.paradoxical" %% "paradox-scala-jackson" % versions.paradoxGlobal,
        "io.paradoxical" %% "paradox-scala-util" % versions.paradoxGlobal
      ) ++ Dependencies.testDeps
    }
  ).dependsOn(
  `finatra-swagger`,
  `finatra-test` % "test",
)

lazy val `finatra-swagger` = project.in(file("finatra-swagger")).
  settings(commonSettings).
  settings(
    name := "finatra-swagger",
    libraryDependencies ++= {
      Dependencies.requiredFinatraDeps ++
      Dependencies.swaggerDeps ++
      Seq()
    }
  )

lazy val `finatra-extensions` = project.in(file(".")).
  settings(commonSettings).settings(
  aggregate in update := false,
  publishArtifact := false
).aggregate(`finatra-swagger`, `finatra-server`, `finatra-test`)

// custom alias to hook in any other custom commands
addCommandAlias("build", "; compile")
