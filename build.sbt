run / fork := true
Global / cancelable := true

ThisBuild / version := "0.2-SNAPSHOT"
ThisBuild / scalaVersion := "3.8.1"
ThisBuild / organization := "ch.contrafactus"

// Add resolver for snapshot dependencies
ThisBuild / resolvers += "Central Portal Snapshots" at "https://central.sonatype.com/repository/maven-snapshots/"

// Setup for Scalafix and SemanticDB
inThisBuild(Seq(
  semanticdbEnabled := true,
  semanticdbVersion := scalafixSemanticdb.revision,
  semanticdbOptions += "-Wunused:imports"
))

addCommandAlias("testAndCoverage", "; clean; coverage; test; coverageReport")

lazy val root = (project in file("."))
  .aggregate(core, examples)
  .settings(
    name := "plotwit-root"
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
      "ch.contrafactus" %% "dimwit-core" % "0.2-SNAPSHOT" changing (),
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
    publish := {},
    publishLocal := {},
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
