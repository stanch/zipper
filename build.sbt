organization := "org.stanch"

name := "zipper"

version := "0.4.0"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.8"

crossScalaVersions := Seq("2.11.8", "2.12.0")

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.2",
  "org.scalatest" %% "scalatest" % "3.0.0" % Test
)

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")

tutSettings

tutTargetDirectory := baseDirectory.value
