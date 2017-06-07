val commonSettings = Seq(
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalacOptions ++= Seq(
    "-feature", "-deprecation",
    "-Xlint", "-Ywarn-unused-import", "-Xfatal-warnings"
  ),
  scalacOptions in (Compile, doc) += "-no-link-warnings"
)

lazy val zipper = crossProject.in(file("."))
  .settings(commonSettings)
  .settings(
    organization := "org.stanch",
    name := "zipper",
    version := "0.5.1",
    licenses += ("MIT", url("http://opensource.org/licenses/MIT")),
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.2",
      "org.scalatest" %%% "scalatest" % "3.0.3" % Test
    )
  )
  .jvmSettings(
    tutSettings,
    tutTargetDirectory := baseDirectory.value.getParentFile
  )

lazy val zipperJVM = zipper.jvm
lazy val zipperJS = zipper.js

lazy val root = project.in(file("."))
  .aggregate(zipperJVM, zipperJS)
  .settings(commonSettings)
  .settings(
    publish := {},
    publishLocal := {}
  )

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")
