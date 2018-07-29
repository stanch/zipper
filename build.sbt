val commonSettings = Seq(
  scalaVersion := "2.11.11",
  crossScalaVersions := Seq("2.11.11", "2.12.2"),
  scalacOptions ++= Seq(
    "-feature", "-deprecation",
    "-Xlint", "-Ywarn-unused-import", "-Xfatal-warnings"
  ),
  scalacOptions in (Compile, doc) += "-no-link-warnings"
) ++ metadata ++ publishing

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

lazy val publishing = Seq(
  useGpg := false,
  usePgpKeyHex("8ED74E385203BEB1"),
  pgpPublicRing := baseDirectory.value.getParentFile / ".gnupg" / "pubring.gpg",
  pgpSecretRing := baseDirectory.value.getParentFile / ".gnupg" / "secring.gpg",
  pgpPassphrase := sys.env.get("PGP_PASS").map(_.toArray),
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  ),
  publishTo := Some(Opts.resolver.sonatypeStaging)
)

lazy val zipper = crossProject.in(file("."))
  .settings(commonSettings)
  .settings(
    name := "zipper",
    version := "0.5.2",
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
    publishLocal := {},
    publishArtifact := false
  )

addCommandAlias("cov", ";coverage; test; coverageOff; coverageReport")
