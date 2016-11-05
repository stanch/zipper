organization := "org.stanch"

name := "zipper"

version := "0.3.1"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.8"

resolvers ++= Seq(
  Resolver.bintrayRepo("stanch", "maven"),
  Resolver.bintrayRepo("drdozer", "maven")
)

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.2.5",
  "org.stanch" %% "reftree" % "0.3.0" % Provided,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "com.lihaoyi" % "ammonite-repl" % "0.5.7" % Test cross CrossVersion.full
)

initialCommands in (Test, console) := """ammonite.repl.Main.run(""); System.exit(0)"""

addCommandAlias("amm", "test:console")

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")

tutSettings

tutTargetDirectory := baseDirectory.value
