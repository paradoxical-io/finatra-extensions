import sbt._
import sbt.Keys._

object BuildConfig {
  object Dependencies {
    val testDeps = Seq(
      "org.scalatest" %% "scalatest" % versions.scalatest,
      "org.mockito" % "mockito-all" % versions.mockito
    ).map(_ % "test")

    val requiredFinatraDeps = Seq(
      "com.twitter" %% "finatra-http" % versions.finatra,
      "org.joda" % "joda-convert" % "1.8"
    )

    val swaggerDeps = Seq(
      "io.swagger" % "swagger-core" % "1.5.16",
      "io.swagger" %% "swagger-scala-module" % "1.0.4",
      "org.webjars" % "swagger-ui" % "3.4.4",
      "net.bytebuddy" % "byte-buddy" % "1.6.1"
    )

    val slf4jSimple = "org.slf4j" % "slf4j-simple" % "1.7.22"
  }

  object Revision {
    lazy val version = System.getProperty("version", "1.0-SNAPSHOT")
  }

  object versions {
    val mockito = "1.10.19"
    val scalatest = "3.0.1"
    val finatra = "17.11.0"
    val guava = "23.4-jre"
    val paradoxGlobal = "1.1"
  }

  def commonSettings() = {
    Seq(
      organization := "io.paradoxical",

      version := BuildConfig.Revision.version,      

      resolvers ++= Seq(
        Resolver.sonatypeRepo("public")
      ),

      scalaVersion := "2.12.4",

      scalacOptions ++= Seq(
        "-deprecation",
        "-encoding", "UTF-8",
        "-feature",
        "-language:existentials",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-language:postfixOps",
        "-language:experimental.macros",
        "-unchecked",
        "-Ywarn-nullary-unit",
        "-Xfatal-warnings",
        "-Ywarn-dead-code",
        "-Xfuture"
      ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 12)) => Seq("-Xlint:-unused")
        case _ => Seq("-Xlint")
      }),

      scalacOptions in (Compile, doc) := scalacOptions.value.filterNot(_ == "-Xfatal-warnings"),
      scalacOptions in (Compile, doc) += "-no-java-comments"
    ) ++ Publishing.publishSettings
  }
}
