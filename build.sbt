val commonScalacOptions =
  Seq(
    "-feature",
    "-deprecation",
    "-Xlint",
    "-Xfatal-warnings"
  )

val Scala212 = "2.12.19"
val Scala213 = "2.13.14"

val commonSettings = Seq(
  scalaVersion := Scala213,
  crossScalaVersions := Seq(Scala212, Scala213),
  scalacOptions ++= {
    scalaVersion.value match {
      case v if v.startsWith("2.12") =>
        Seq(
          "-Ypartial-unification",
          "-Ywarn-unused-import"
        ) ++ commonScalacOptions
      case _ =>
        commonScalacOptions
    }
  },
  version := "0.6.0"
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

lazy val zipper = crossProject(JSPlatform, JVMPlatform).in(file("."))
  .settings(commonSettings)
  .settings(
    name := "zipper",
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % "2.3.10",
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
