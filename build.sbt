name := "zipper"

scalaVersion := "2.11.7"

resolvers += Resolver.bintrayRepo("stanch", "maven")

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.2.5",
  "org.stanch" %% "reftree" % "0.1.3" % Provided,
  "org.scalatest" %% "scalatest" % "2.2.6" % Test,
  "com.lihaoyi" % "ammonite-repl" % "0.5.4" % Test cross CrossVersion.full
)

initialCommands in (Test, console) := """ammonite.repl.Main.run(""); System.exit(0)"""

addCommandAlias("amm", "test:console")

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")

tutSettings

tutTargetDirectory := baseDirectory.value
