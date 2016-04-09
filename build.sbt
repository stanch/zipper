organization := "org.stanch"

name := "zipper"

version := "0.2.0"

licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion := "2.11.8"

resolvers += Resolver.bintrayRepo("stanch", "maven")

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
