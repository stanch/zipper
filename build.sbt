val Scala212 = "2.12.19"
val Scala213 = "2.13.14"
val Scala3 = "3.3.3"

val commonSettings = Seq(
  scalaVersion := Scala213,
  crossScalaVersions := Seq(Scala212, Scala213, Scala3),
  scalacOptions ++= {
    val commonScalacOptions =
      Seq(
        "-feature",
        "-deprecation",
        "-Xfatal-warnings"
      )
    val scala2Options =
      Seq(
        "-Xlint"
      )

    scalaVersion.value match {
      case v if v.startsWith("2.12") =>
        Seq(
          "-Ypartial-unification",
          "-Ywarn-unused-import"
        ) ++ commonScalacOptions ++ scala2Options
      case v if v.startsWith("2.13") =>
        commonScalacOptions ++ scala2Options
      case _ =>
        commonScalacOptions
    }
  }
) ++ metadata

lazy val metadata = Seq(
  organization := "io.github.stanch",
  homepage := Some(url("https://stanch.github.io/zipper/")),
  scmInfo := Some(ScmInfo(
    url("https://github.com/stanch/zipper"),
    "scm:git@github.com:stanch/zipper.git"
  )),
  developers := List(Developer(
    id="stanch",
    name="Nick Stanchenko",
    email="nick.stanch@gmail.com",
    url=url("https://github.com/stanch")
  )),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
)

lazy val zipper = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .settings(commonSettings)
  .settings(
    name := "zipper",
    libraryDependencies += {
      scalaVersion.value match {
        case v if v.startsWith("2") =>
          "com.chuusai" %%% "shapeless" % "2.3.10"
        case _ =>
          "org.typelevel" %% "shapeless3-deriving" % "3.4.1"
      }
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest-flatspec" % "3.2.18" % Test,
      "org.scalatest" %%% "scalatest-shouldmatchers" % "3.2.18" % Test
    )
  )

lazy val zipperJVM = zipper.jvm
lazy val zipperJS = zipper.js

lazy val docs = project
  .in(file("docs-gen"))
  .enablePlugins(MdocPlugin, BuildInfoPlugin)
  .dependsOn(zipperJVM)
  .settings(commonSettings)
  .settings(
    name := "zipper-docs",
    moduleName := "zipper-docs",
    (publish / skip) := true,
    mdoc := (Compile / run).evaluated,
    (Compile / mainClass) := Some("zipper.Docs"),
    (Compile / resources) ++= {
      List((ThisBuild / baseDirectory).value / "docs")
    },
    buildInfoKeys := Seq[BuildInfoKey](version),
    buildInfoPackage := "zipper.build"
  )

lazy val root = project.in(file("."))
  .aggregate(zipperJVM, zipperJS, docs)
  .settings(commonSettings)
  .settings(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")
