run / fork := true
Global / cancelable := true

ThisBuild / version := "0.3-SNAPSHOT"
ThisBuild / scalaVersion := "3.9.0"
ThisBuild / organization := "ch.contrafactus"

ThisBuild / versionScheme := Some("early-semver")

// Add resolver for snapshot dependencies
ThisBuild / resolvers += "Central Portal Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"

// Publishing to Sonatype Central
ThisBuild / sonatypeCredentialHost := "central.sonatype.com"
ThisBuild / publishTo := {
  if (isSnapshot.value)
    Some("central-snapshots" at "https://central.sonatype.com/repository/maven-snapshots/")
  else
    sonatypePublishToBundle.value
}
ThisBuild / publishMavenStyle := true
ThisBuild / homepage := Some(url("https://github.com/dimwit-dev/plotwit"))
ThisBuild / licenses := List("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/dimwit-dev/plotwit"),
    "scm:git@github.com:dimwit-dev/plotwit.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "dimwit-dev",
    name = "PlotWit Contributors",
    email = "",
    url = url("https://github.com/dimwit-dev")
  )
)

// Setup for Scalafix and SemanticDB
inThisBuild(Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  semanticdbOptions += "-Wunused:imports"
))

addCommandAlias("testAndCoverage", "; clean; coverage; test; coverageReport")

// alias for uploading core to Sonatype Central
addCommandAlias("sonaUploadCore", "; project core; sonatypeCentralUpload; project root")

lazy val root = (project in file("."))
  .aggregate(core, examples)
  .settings(
    name := "plotwit-root",
    publish / skip := true,
    publishLocal / skip := true,
    publishArtifact := false
  )

lazy val core = (project in file("core"))
  .settings(
    name := "plotwit-core",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalacheck" %% "scalacheck" % "1.18.0" % Test,
      "org.scalatestplus" %% "scalacheck-1-18" % "3.2.19.0" % Test,
      "dev.scalapy" %% "scalapy-core" % "0.5.3",
      // DimWit
      "ch.contrafactus" %% "dimwit-core" % "0.3-SNAPSHOT" changing (),
      // Viz 4 Scala
      "io.github.quafadas" %% "dedav4s" % "0.10.5"
    ),
    Compile / packageSrc / publishArtifact := true,
    Compile / packageDoc / publishArtifact := true
  )

// Examples subproject
lazy val examples = (project in file("examples"))
  .dependsOn(core)
  .settings(
    name := "plotwit-examples",
    libraryDependencies ++= Seq(
      "dev.scalapy" %% "scalapy-core" % "0.5.3"
    ),
    fork := true,
    // Don't publish examples
    publish / skip := true,
    publishLocal / skip := true,
    publishArtifact := false,
    // Examples source directory
    Compile / scalaSource := baseDirectory.value,
    Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources"
  )

// Renders the plots of the README gallery into docs/plots
lazy val renderPlots = taskKey[Unit]("Render every plot of the README gallery into docs/plots")

renderPlots := Def.taskDyn {
  val outDir = (ThisBuild / baseDirectory).value / "docs" / "plots"
  (examples / Compile / runMain).toTask(s" plotwit.examples.plots.renderPlots ${outDir.getAbsolutePath}")
}.value
